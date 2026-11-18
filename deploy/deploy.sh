#!/usr/bin/env bash
set -Eeuo pipefail

REPO_DIR="/opt/PROGI_projekt/"
BRANCH="master"
LOCK_FILE="/var/lock/PROGI-deploy.lock"
BUN="/home/mateo/.bun/bin/bun"

# Prevent overlapping deployments, even if invoked outside the webhook.
exec 9>"$LOCK_FILE"
if ! flock -n 9; then
    echo "Another deployment is running; skipping."
    exit 0
fi

SCRIPT_DIR="$(dirname "$(realpath "$0")")"

cd "$REPO_DIR"

echo "Fetching latest code..."
git fetch origin "$BRANCH"

# Don't reset or rebuild if this checkout is already up to date.
LOCAL_COMMIT=$(git rev-parse HEAD)
REMOTE_COMMIT=$(git rev-parse "origin/$BRANCH")

# if [[ "$LOCAL_COMMIT" == "$REMOTE_COMMIT" ]]; then
#     echo "Already up to date at $LOCAL_COMMIT"
#     exit 0
# fi

echo "Deploying $REMOTE_COMMIT"

git fetch --prune origin
git switch --force "$BRANCH"
git reset --hard "origin/$BRANCH"


FRONTEND_STAGE_DIR="$SCRIPT_DIR/frontend_staging/"

# Validate paths before any destructive operation
[[ -n "$SCRIPT_DIR" && -n "$REPO_DIR" ]] || exit 1
[[ "$FRONTEND_STAGE_DIR" == "$SCRIPT_DIR/"* ]] || exit 1
[[ "$FRONTEND_STAGE_DIR" != "/" ]] || exit 1
[[ "$FRONTEND_STAGE_DIR" != "$SCRIPT_DIR" ]] || exit 1

pushd ./frontend

# 172800s = 2 dana
"$BUN" install --production --no-save --minimum-release-age=172800

"$BUN" vite build

DIST_DIR="$REPO_DIR/frontend/dist/"

[[ -d "$DIST_DIR" ]] || {
    echo "ERROR: frontend dist directory missing" >&2
    exit 1
}

mkdir -p -- "$FRONTEND_STAGE_DIR"
rm -rf -- "$FRONTEND_STAGE_DIR"/*
cp -a -- "$DIST_DIR/." "$FRONTEND_STAGE_DIR/"
date > "$FRONTEND_STAGE_DIR/.deploy_ready"

echo -e "\x1b[1;33mFRONTEND\x1b[0;0m deployed!"
popd


pushd ./db

podman compose up -d
# docker compose run --rm migrate

echo -e "\x1b[1;33mDB\x1b[0;0m deployed!"
popd


pushd ./backend/LetsDoSports

sudo -n /usr/bin/systemctl restart PROGI-deploy-backend.service
/usr/bin/systemctl status PROGI-deploy-backend.service --no-pager

echo -e "\x1b[1;33mBACKEND\x1b[0;0m deployed!"
popd

echo "Deployment finished at $(date +"%H:%M %d.%m.%Y") ($REMOTE_COMMIT)"

