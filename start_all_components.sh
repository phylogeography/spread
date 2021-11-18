#!/bin/bash

set -evx

# tmux kill-session -t spread

# start dockerized components
tmux new-session -s spread -d 'source env/dev; bash -i';
tmux send-keys -t "spread:0" 'docker-compose -f deployments/dev/docker-compose.yml up' Enter;

# start worker thread
tmux new-window -t "spread:1";
tmux send-keys -t "spread:1" 'source env/dev' Enter;
tmux send-keys -t "spread:1" 'clj -A:run-worker' Enter;

# start API
# NOTE: api needs two more ENV vars exported for the login to work properly
# PRIVATE_KEY_DEV and GOOGLE_CLIENT_SECRET
# which are not under source version control for security reasons
# as for them before running the software locally
tmux new-window -t "spread:2";
tmux send-keys -t "spread:2" 'source env/dev' Enter;
tmux send-keys -t "spread:2" 'clj -A:run-api' Enter;

# start frontend
tmux new-window -t "spread:3";
tmux send-keys -t "spread:3" 'source env/dev' Enter;
tmux send-keys -t "spread:3" 'yarn watch' Enter;

# start viewer
tmux new-window -t "spread:4";
tmux send-keys -t "spread:4" 'source env/dev' Enter;
tmux send-keys -t "spread:4" 'yarn watch:viewer' Enter;

tmux a;

exit $?
