git_webhook: webhook_handler.go
	go build -o git_webhook webhook_handler.go


# CI/CD Setup

# ============================================================
# PROGI deployment Makefile
# ============================================================

SHELL := /bin/bash
.SHELLFLAGS := -Eeuo pipefail -c
.ONESHELL:
MAKEFLAGS += --no-print-directory

# Resolve the directory containing this Makefile,
# independently of the current working directory.
MAKEFILE_DIR := $(abspath $(dir $(lastword $(MAKEFILE_LIST))))

# ============================================================
# CONFIGURATION
# ============================================================

DEPLOY_USER    := # npr. deploy
DEPLOY_DIR     ?= /home/$(DEPLOY_USER)/deploy
PROJECT_DIR    ?= /opt/projekt
FRONTEND_DIR   ?= /var/www/lets-do-sports

WEBHOOK_PORT   ?= 5999
BACKEND_PORT   ?= 8080
JAVA_HOME      ?= /lib/jvm/default
BUN            ?= /home/$(DEPLOY_USER)/.bun/bin/bun

STAGING_DIR    := $(DEPLOY_DIR)/frontend_staging
SERVICE_DIR    := $(MAKEFILE_DIR)/systemd-services

SYSTEMD_DIR    ?= /etc/systemd/system
SUDOERS_FILE   ?= /etc/sudoers.d/letsdosports # THIS FILE GET'S DELETED!!
CADDY_FILE     ?= /etc/caddy/Caddyfile        # THIS FILE GET'S DELETED!!

WEBHOOK_SERVICE  := PROGI-github-webhook.service
FRONTEND_SERVICE := PROGI-deploy-frontend.service
BACKEND_SERVICE  := PROGI-deploy-backend.service

GO    ?= go
SUDO  ?= sudo

.PHONY: install-webhook setup validate packages directories
.PHONY: podman env check-env check-project check-scripts
.PHONY: install-services sudoers caddy enable start-services
.PHONY: stop-services restart status clean help

# ============================================================
# VALIDATION
# ============================================================

REQUIRED_VARS := DEPLOY_USER PROJECT_DIR FRONTEND_DIR

$(foreach var,$(REQUIRED_VARS),\
    $(if $(strip $($(var))),,\
        $(error Required variable $(var) is not set, run like: \`make $(var)=value\` to set it)))

validate: check-project check-env check-scripts check-services
	@echo -e "\033[32;1mConfiguration OK.\033[0m"

# ============================================================
# PACKAGES
# ============================================================

packages:
	@echo "Installing system dependencies..."

	$(SUDO) apt update

	$(SUDO) apt install -y --no-reinstall \
		inotify-tools \
		rsync \
		git \
		curl \
		ca-certificates \
		openssl \
		make \
		golang-go \
		sudo \
		util-linux \
		podman \
		podman-compose \
		dbus-user-session \
		uidmap \
		slirp4netns \
		fuse-overlayfs

	curl -fsSL https://bun.com/install | sudo -u $(DEPLOY_USER) bash

	$(SUDO) apt install --yes debian-keyring debian-archive-keyring apt-transport-https curl
	curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | $(SUDO) gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
	curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | $(SUDO) tee /etc/apt/sources.list.d/caddy-stable.list
	$(SUDO) chmod o+r /usr/share/keyrings/caddy-stable-archive-keyring.gpg
	$(SUDO) chmod o+r /etc/apt/sources.list.d/caddy-stable.list
	$(SUDO) apt update
	$(SUDO) apt install caddy

	@echo -e "\033[36;1mPackages installed.\033[0m"

# ============================================================
# DEPLOY USER
# ============================================================

user:
	@echo "Ensuring deployment user and group exist..."
	if ! getent group "$(DEPLOY_USER)" >/dev/null; then
		$(SUDO) groupadd "$(DEPLOY_USER)"
	fi
	if ! id -u "$(DEPLOY_USER)" >/dev/null 2>&1; then
		$(SUDO) useradd --create-home --gid "$(DEPLOY_USER)" --shell /bin/bash "$(DEPLOY_USER)"
		@echo -e "\033[36;1mDeployment user added.\033[0m"
	elif [ "$$(id -gn "$(DEPLOY_USER)")" != "$(DEPLOY_USER)" ]; then
		$(SUDO) usermod --gid "$(DEPLOY_USER)" "$(DEPLOY_USER)"
	fi
	$(SUDO) install -d -o "$(DEPLOY_USER)" -g "$(DEPLOY_USER)" "/home/$(DEPLOY_USER)"


# ============================================================
# DIRECTORIES
# ============================================================

directories:
	@echo "Creating deployment directories..."

	$(SUDO) install -d \
		-o "$(DEPLOY_USER)" -g "$(DEPLOY_USER)" \
		"$(DEPLOY_DIR)" \
		"$(STAGING_DIR)" \
		"$(PROJECT_DIR)"

	$(SUDO) install -d \
		-o root -g root \
		"$(FRONTEND_DIR)"

	# deploy.sh opens this file using flock.
	# Preserve an existing lock file rather than truncating it.
	if [ ! -e /var/lock/PROGI-deploy.lock ]; then
		$(SUDO) install \
			-o "$(DEPLOY_USER)" -g "$(DEPLOY_USER)" \
			-m 0644 /dev/null /var/lock/PROGI-deploy.lock
	else
		$(SUDO) chown "$(DEPLOY_USER):$(DEPLOY_USER)" \
			/var/lock/PROGI-deploy.lock
	fi

	@echo -e "\033[36;1mDirectories created.\033[0m"

# ============================================================
# PODMAN / SYSTEMD USER SESSION
# ============================================================

podman:
	@echo "Configuring systemd user session for $(DEPLOY_USER)..."

	user_uid=$$(id -u "$(DEPLOY_USER)")

	# Enable user manager at boot and keep it alive after logout.
	$(SUDO) loginctl enable-linger "$(DEPLOY_USER)"

	# Start the user's systemd manager.
	$(SUDO) systemctl start "user@$$user_uid.service"

	# Wait for the user D-Bus socket.
	for i in {1..20}; do
		if $(SUDO) test -S "/run/user/$$user_uid/bus"; then
			break
		fi
		sleep 0.5
	done

	$(SUDO) test -S "/run/user/$$user_uid/bus" || {
		echo -e "\033[31mERROR: User D-Bus socket not available.\033[0m"
		exit 1
	}

	# Enable and start Podman socket as the deployment user.
	$(SUDO) -u "$(DEPLOY_USER)" env \
		XDG_RUNTIME_DIR="/run/user/$$user_uid" \
		DBUS_SESSION_BUS_ADDRESS="unix:path=/run/user/$$user_uid/bus" \
		systemctl --user enable --now podman.socket

	# Verify Podman socket.
	$(SUDO) test -S "/run/user/$$user_uid/podman/podman.sock" || {
		echo -e "\033[31mERROR: Podman socket was not created.\033[0m"
		exit 1
	}

	@echo -e "\033[36;1mPodman user session configured.\033[0m"

# ============================================================
# ENVIRONMENT
# ============================================================

env:
	@echo "Configuring environment..."

	if [ ! -f "$(DEPLOY_DIR)/.env" ]; then
		user_uid=$$(id -u "$(DEPLOY_USER)")
		umask 077

		printf '%s\n' \
			"PORT=$(WEBHOOK_PORT)" \
			"GITHUB_WEBHOOK_SECRET=$$(openssl rand -hex 32)" \
			"DEPLOY_SCRIPT_PATH=$(DEPLOY_DIR)/deploy.sh" \
			"XDG_RUNTIME_DIR=/run/user/$$user_uid" \
			"DBUS_SESSION_BUS_ADDRESS=unix:path=/run/user/$$user_uid/bus" \
			"DOCKER_HOST=unix:///run/user/$$user_uid/podman/podman.sock" \
			"JAVA_HOME=$(JAVA_HOME)" \
			| $(SUDO) tee "$(DEPLOY_DIR)/.env" > /dev/null

		$(SUDO) chown "$(DEPLOY_USER):$(DEPLOY_USER)" \
			"$(DEPLOY_DIR)/.env"

		$(SUDO) chmod 600 "$(DEPLOY_DIR)/.env"

		@echo -e "\033[36;1m.env created.\033[0m"
	else
		@echo -e "\033[33m.env already exists. Preserving existing configuration.\033[0m"
	fi

check-env:
	@test -f "$(DEPLOY_DIR)/.env" || {
		echo -e "\033[31mERROR: Deploy .env file is missing. ($(DEPLOY_DIR)/.env)\033[0m"
		exit 1
	}

	@$(SUDO) grep -q '^GITHUB_WEBHOOK_SECRET=.' "$(DEPLOY_DIR)/.env" || {
		echo -e "\033[31mERROR: GITHUB_WEBHOOK_SECRET is missing. (from $(DEPLOY_DIR)/.env)\033[0m"
		exit 1
	}

	@echo -e "\033[32;1mDeploy .env configuration OK.\033[0m"

# ============================================================
# DEPLOYMENT SCRIPTS
# ============================================================

install-scripts:
	@echo "Installing deployment scripts..."

	for script in \
		"deploy.sh" \
		"deploy_frontend.sh"; do

		test -f "$(MAKEFILE_DIR)/$$script" || {
			echo -e "\033[31mERROR: Missing script file: $$script\033[0m"
			exit 1
		}

		tmp=$$(mktemp)
		trap 'rm -f "$$tmp"' EXIT

		# Adapt the supplied script files to the actual Makefile config
		sed \
			-e 's|/opt/PROGI_deployment|$(DEPLOY_DIR)|g' \
			-e 's|/opt/PROGI_projekt|$(PROJECT_DIR)|g' \
			-e 's|/var/www/lets-do-sports|$(FRONTEND_DIR)|g' \
			-e 's|/home/mateo/.bun/bin/bun|$(BUN)|g' \
			-e 's|mateo|$(DEPLOY_USER)|g' \
			"$(MAKEFILE_DIR)/$$script" > "$$tmp"

		$(SUDO) install -m 0755 -o "$(DEPLOY_USER)" -g "$(DEPLOY_USER)" \
			"$$tmp" "$(DEPLOY_DIR)/$$script"

		rm -f "$$tmp"
	done

	@echo -e "\033[36;1mDeployment scripts installed.\033[0m"


check-scripts:
	@echo "Checking deployment scripts..."

	$(SUDO) -u $(DEPLOY_USER) test -f "$(DEPLOY_DIR)/git_webhook"
	$(SUDO) -u $(DEPLOY_USER) test -f "$(DEPLOY_DIR)/deploy.sh"
	$(SUDO) -u $(DEPLOY_USER) test -f "$(DEPLOY_DIR)/deploy_frontend.sh"

	# Ensure scripts can be executed by systemd/webhook.
	$(SUDO) chmod 0755 \
		"$(DEPLOY_DIR)/deploy.sh" \
		"$(DEPLOY_DIR)/deploy_frontend.sh"

	@echo -e "\033[32;1mDeployment scripts OK.\033[0m"

# ============================================================
# PROJECT VALIDATION
# ============================================================

check-project: 	DB_ENV_FILE := $(PROJECT_DIR)/db/.env
check-project: 	BACKEND_ENV_FILE := $(PROJECT_DIR)/backend/LetsDoSports/.env
check-project:
	@echo "Checking project repository and runtime dependencies..."

	$(SUDO) -u $(DEPLOY_USER) git -C "$(PROJECT_DIR)" rev-parse --is-inside-work-tree >/dev/null || {
		echo -e "\033[31mERROR: No Git repository found in $(PROJECT_DIR).\033[0m"
		exit 1
	}

	$(SUDO) -u $(DEPLOY_USER) git -C "$(PROJECT_DIR)" remote get-url origin >/dev/null || {
		echo -e "\033[31mERROR: Git remote 'origin' is missing.\033[0m"
		exit 1
	}

	$(SUDO) -u $(DEPLOY_USER) git -C "$(PROJECT_DIR)" show-ref \
		--verify --quiet refs/heads/master || {
		echo -e "\033[31mERROR: Local master branch is missing.\033[0m"
		exit 1
	}

	$(SUDO) -u $(DEPLOY_USER) test -x "$(BUN)" || {
		echo -e "\033[31mERROR: Bun not found or not executable: $(BUN)\033[0m"
		exit 1
	}

	$(SUDO) -u $(DEPLOY_USER) test -x "$(JAVA_HOME)/bin/java" || {
		echo -e "\033[31mERROR: Java not found in JAVA_HOME: $(JAVA_HOME)\033[0m"
		exit 1
	}

	$(SUDO) -u $(DEPLOY_USER) test -d "$(PROJECT_DIR)/frontend" || {
		echo -e "\033[31mERROR: frontend directory not found in $(PROJECT_DIR)\033[0m"
	}
	$(SUDO) -u $(DEPLOY_USER) test -d "$(PROJECT_DIR)/db" || {
		echo -e "\033[31mERROR: db directory not found in $(PROJECT_DIR)\033[0m"
	}

	$(SUDO) -u $(DEPLOY_USER) test -d "$(PROJECT_DIR)/backend/LetsDoSports" || {
		echo -e "\033[31mERROR: backend/LetsDoSports directory not found in $(PROJECT_DIR)\033[0m"
	}


	# Verify that Podman Compose is available to the deployment user.
	user_uid=$$(id -u "$(DEPLOY_USER)")

	$(SUDO) -u "$(DEPLOY_USER)" env \
		XDG_RUNTIME_DIR="/run/user/$$user_uid" \
		DBUS_SESSION_BUS_ADDRESS="unix:path=/run/user/$$user_uid/bus" \
		podman compose version || {
		echo -e "\033[31mERROR: \`podman compose\` cannot be started033[0m"
		exit 1
	}

	pushd $(PROJECT_DIR)/db

	$(SUDO) -u "$(DEPLOY_USER)" env \
		XDG_RUNTIME_DIR="/run/user/$$user_uid" \
		DBUS_SESSION_BUS_ADDRESS="unix:path=/run/user/$$user_uid/bus" \
		podman compose config > /dev/null || {
		echo -e "\033[31mERROR: db .env file was not set properly $(PROJECT_DIR)/db/.env\033[0m"
		exit 1
	}

	popd

	@[[ -f "$(DB_ENV_FILE)" ]] || {
		echo -e "\033[31mERROR: .env datoteka za bazu podataka nije pronađena: $(DB_ENV_FILE)\033[0m"
		exit 1;
	}

	@[[ -f "$(BACKEND_ENV_FILE)" ]] || {
		echo -e "\033[31mERROR: .env datoteka za backend nije pronađena: $(BACKEND_ENV_FILE)\033[0m"
		exit 1;
	}

	@missing_vars=""
	@for var in POSTGRES_DB APP_DB_USER APP_DB_PASSWORD GOOGLE_CLIENT_ID GOOGLE_CLIENT_SECRET BACKEND_URL FRONTEND_URL; do
		if ! $(SUDO) grep -q "^$${var}=." "$(DB_ENV_FILE)" "$(BACKEND_ENV_FILE)"; then
			echo -e "\033[31mERROR: Nužno je postaviti $${var} varijablu u .env datoteci.\033[0m"
			missing_vars="yup"
		fi;
	done

	[[ "$$missing_vars" == "yup" ]] && exit 1

	@for var in BACKEND_URL FRONTEND_URL; do
		if ! $(SUDO) grep -q "^$${var}=https://letsdosports.eu" "$(DB_ENV_FILE)" "$(BACKEND_ENV_FILE)"; then
			echo -e "\033[31mERROR: Varijabla $${var} mora biti postavljena na \`https://letsdosports.eu\`.\033[0m"
			exit 1;
		fi;
	done

	@echo "Varijable okruženja su postavljene za produkciju."

	@echo -e "\033[32;1mProject and runtime dependencies OK.\033[0m"

# ============================================================
# BUILD
# ============================================================

install-webhook: webhook_handler.go
	@echo "Building GitHub webhook..."

	$(GO) build -o "$(MAKEFILE_DIR)/git_webhook" webhook_handler.go
	trap 'rm -f "$(MAKEFILE_DIR)/git_webhook"' EXIT

	@echo "Webhook build completed."

	@echo "Installing GitHub webhook in $(DEPLOY_DIR)"
	$(SUDO) install -m 0755 -o "$(DEPLOY_USER)" -g "$(DEPLOY_USER)" \
	"$(MAKEFILE_DIR)/git_webhook" "$(DEPLOY_DIR)/git_webhook"
	@echo -e "\033[36;1mGitHub webhook install completed.\033[0m"


# ============================================================
# SYSTEMD SERVICES
# ============================================================

install-services: stop-services
	@echo "Installing systemd services..."

	for service in \
		"$(WEBHOOK_SERVICE)" \
		"$(FRONTEND_SERVICE)" \
		"$(BACKEND_SERVICE)"; do

		test -f "$(SERVICE_DIR)/$$service" || {
			echo -e "\033[31mERROR: Missing service file: $$service\033[0m"
			exit 1
		}

		tmp=$$(mktemp)
		trap 'rm -f "$$tmp"' EXIT

		# Adapt the supplied service files to the actual Makefile
		# directory and configured project paths.
		sed \
			-e 's|/opt/PROGI_deployment|$(DEPLOY_DIR)|g' \
			-e 's|/opt/PROGI_projekt|$(PROJECT_DIR)|g' \
			-e 's|/var/www/lets-do-sports|$(FRONTEND_DIR)|g' \
			-e 's|mateo|$(DEPLOY_USER)|g' \
			"$(SERVICE_DIR)/$$service" > "$$tmp"

		$(SUDO) install -m 0644 \
			"$$tmp" "$(SYSTEMD_DIR)/$$service"

		rm -f "$$tmp"
	done

	$(SUDO) systemctl daemon-reload

	@echo -e "\033[36;1mSystemd services installed.\033[0m"

# ============================================================
# SUDOERS
# ============================================================

sudoers:
	@echo "Configuring sudo permissions..."

	tmp=$$(mktemp)
	trap 'rm -f "$$tmp"' EXIT

	printf '%s ALL=(root) NOPASSWD: /usr/bin/systemctl restart %s\n' \
		"$(DEPLOY_USER)" "$(BACKEND_SERVICE)" > "$$tmp"

	$(SUDO) visudo -cf "$$tmp"

	$(SUDO) install \
		-o root -g root -m 0440 \
		"$$tmp" "$(SUDOERS_FILE)"

	$(SUDO) visudo -c

	@echo -e "\033[36;1mSudoers configured successfully.\033[0m"

# ============================================================
# CADDY
# ============================================================
caddy:
	@echo "Installing Caddy configuration..."

	command -v caddy >/dev/null || {
			echo -e "\033[31mERROR: Caddy is not installed.\033[0m"
			exit 1
	}

	@tmp=$$(mktemp); \
	trap 'rm -f "$$tmp"' EXIT; \
	printf '%s\n' \
	'letsdosports.eu www.letsdosports.eu {' \
	'       root * $(FRONTEND_DIR)' \
	'       encode zstd gzip' \
	'       # GitHub webhook' \
	'       @webhook path /github_webhook' \
	'       handle @webhook {' \
	'               reverse_proxy 127.0.0.1:$(WEBHOOK_PORT)' \
	'       }' \
	'       handle /api/* {' \
	'               reverse_proxy http://127.0.0.1:$(BACKEND_PORT)' \
	'       }' \
	'       handle /oauth2/* {' \
	'               reverse_proxy http://127.0.0.1:$(BACKEND_PORT)' \
	'       }' \
	'       handle {' \
	'               try_files {path} /index.html' \
	'               file_server' \
	'       }' \
	'}' > "$$tmp"; \

	$(SUDO) caddy validate --config "$$tmp" --adapter caddyfile && \
	$(SUDO) install -o root -g root -m 0644 "$$tmp" "$(CADDY_FILE)" && \
	$(SUDO) caddy reload --config "$(CADDY_FILE)" && \
	echo -e "\033[36;1mCaddy configured successfully.\033[0m"

clone-repo:
	@$(SUDO) -u $(DEPLOY_USER) git -C "$(PROJECT_DIR)" rev-parse --is-inside-work-tree >/dev/null || {
		$(SUDO) -u $(DEPLOY_USER) git clone https://github.com/MKos-FER/progi_projekt.git $(PROJECT_DIR) \
		&& 	echo -e "\033[36;1mProject repository cloned.\033[0m"
	}

# ============================================================
# SERVICE MANAGEMENT
# ============================================================

enable-services:
	@echo "Enabling deployment services..."

	$(SUDO) systemctl enable "$(WEBHOOK_SERVICE)"
	$(SUDO) systemctl enable "$(FRONTEND_SERVICE)"
	$(SUDO) systemctl enable "$(BACKEND_SERVICE)"

start-services:
	@echo "Starting deployment services..."

	$(SUDO) systemctl start "$(WEBHOOK_SERVICE)"
	$(SUDO) systemctl start "$(FRONTEND_SERVICE)"
	# Backend is started by deploy.sh when needed.

stop-services:
	$(SUDO) systemctl is-active "$(WEBHOOK_SERVICE)" && $(SUDO) systemctl stop "$(WEBHOOK_SERVICE)" || true
	$(SUDO) systemctl is-active "$(FRONTEND_SERVICE)" && $(SUDO) systemctl stop "$(FRONTEND_SERVICE)" || true
	$(SUDO) systemctl is-active "$(BACKEND_SERVICE)" && $(SUDO) systemctl stop "$(BACKEND_SERVICE)" || true

check-services:
	$(SUDO) systemctl status "$(WEBHOOK_SERVICE)" --no-pager
	$(SUDO) systemctl status "$(FRONTEND_SERVICE)" --no-pager
	$(SUDO) systemctl status "$(BACKEND_SERVICE)" --no-pager || true
	$(SUDO) systemctl is-enabled "$(BACKEND_SERVICE)"

# ============================================================
# COMPLETE INITIAL SETUP
# ============================================================

setup:
	@echo "=========================================="
	echo " PROGI deployment - INITIAL SETUP"
	echo "=========================================="

	$(MAKE) user

	$(MAKE) packages

	$(MAKE) directories

	$(MAKE) env
	$(MAKE) check-env

	$(MAKE) podman

	$(MAKE) clone-repo

	$(MAKE) install-scripts

	$(MAKE) install-webhook

	$(MAKE) check-scripts

	$(MAKE) install-services

	$(MAKE) sudoers

	$(MAKE) caddy

	$(MAKE) enable-services
	$(MAKE) start-services

	$(MAKE) check-project

	@echo "=========================================="
	echo -e "\033[36;1m PROGI deployment setup completed!\033[0m"
	echo "=========================================="

# ============================================================
# CLEANUP
# ============================================================

clean:
	rm -f "$(DEPLOY_DIR)/git_webhook"

# ============================================================
# HELP
# ============================================================

help:
	@echo "Available targets:"
	@echo ""
	@echo "  make                      Build webhook"
	@echo "  make setup                Complete initial server setup"
	@echo "  make validate             Validate configuration"
	@echo "  make packages             Install system dependencies"
	@echo "  make directories          Create deployment directories"
	@echo "  make podman               Configure user systemd and Podman"
	@echo "  make env                  Generate .env if missing"
	@echo "  make check-env            Validate .env"
	@echo "  make check-scripts        Validate deployment scripts"
	@echo "  make check-project        Validate Git repository and runtime"
	@echo "  make install-webhook      Build and install GitHub webhook"
	@echo "  make install-services     Install systemd services"
	@echo "  make sudoers              Configure sudo permissions"
	@echo "  make caddy                Configure Caddy"
	@echo "  make enable               Enable systemd services"
	@echo "  make start-services       Start webhook and frontend"
	@echo "  make stop                 Stop deployment services"
	@echo "  make restart              Restart webhook and frontend"
	@echo "  make check-services       Show service statuses"
	@echo "  make clean                Remove compiled webhook"
	@echo "  make help                 Show this help"
