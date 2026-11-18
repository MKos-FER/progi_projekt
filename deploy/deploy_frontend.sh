#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(dirname "$(realpath "$0")")"
WATCH_DIR="$SCRIPT_DIR/frontend_staging"
WEB_DIR="${WEB_DIR:?WEB_DIR must be set}"

[[ -d "$WATCH_DIR" ]] || {
    echo "ERROR: staging directory missing: $WATCH_DIR" >&2
    exit 1
}

mkdir -p -- "$WEB_DIR"

deploy() {
    sleep 2

    [[ -s "$WATCH_DIR/index.html" ]] || {
        echo "ERROR: staging build is incomplete" >&2
        return 1
    }

    echo "Deploying frontend to $WEB_DIR..."

    rsync -a --delete \
        --exclude='.deploy_ready' \
        "$WATCH_DIR/" "$WEB_DIR/"

    echo "Frontend deployed."
}

echo "Watching $WATCH_DIR"

while true; do
    inotifywait -m -e close_write,create,moved_to \
        --format '%f' "$WATCH_DIR" |
    while IFS= read -r changed; do
        [[ "$changed" == ".deploy_ready" ]] || continue
        [[ -e "$WATCH_DIR/.deploy_ready" ]] || continue

        deploy || echo "ERROR: deployment failed" >&2
    done

    echo "Watcher stopped; restarting in 2 seconds..." >&2
    sleep 2
done
