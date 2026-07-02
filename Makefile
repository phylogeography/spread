.PHONY: help # Show help for each of the Makefile recipes
help:
	@grep -E '^\.PHONY: .+ #' Makefile | sort | while read -r l; do printf "\033[1;32m%s\033[00m:%s\n" "$$(echo "$$l" | sed -E 's/^\.PHONY: ([^ ]+).*/\1/')" "$$(echo "$$l" | cut -f 2- -d'#')"; done

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

.PHONY: deploy-images # build and push the CI docker images (needs DOCKER_USERNAME/DOCKER_PASSWORD)
deploy-images:
	cd docker-images && for img in clj cljs deploy; do ./deploy.sh $$img || exit 1; done
