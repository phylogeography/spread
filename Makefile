.PHONY: help
help: # Show help for each of the Makefile recipes
	@grep -E '^[a-zA-Z0-9 -]+:.*#'  Makefile | sort | while read -r l; do printf "\033[1;32m$$(echo $$l | cut -f 1 -d':')\033[00m:$$(echo $$l | cut -f 2- -d'#')\n"; done

.PHONY: run-api # starts the api server
run-api:
	clj -M:run-api

.PHONY: run-worker # starts the async worker
run-worker:
	clj -M:run-worker

.PHONY: debug-api # use flowstorm to debug the api
debug-api:
	clj -X:dbg flow-storm.api/cli-run :instrument-ns '#{"aws." "api."}' :fn-symb 'api.main/-main' :fn-args '[]'

.PHONY: debug-worker # use flowstorm to debug the worker
debug-worker:
	clj -X:dbg flow-storm.api/cli-run :instrument-ns '#{"worker." "aws." "api."}' :fn-symb 'worker.main/-main' :fn-args '[]'

.PHONY: lint-clj # lint all clj and cljs sources
lint-clj:
	clj -M:lint --lint src/clj src/cljc

.PHONY: unit-tests # lint all unit tests
unit-tests:
	./bin/kaocha unit
