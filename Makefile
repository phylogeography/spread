run-api:
	clj -M:run-api

run-worker:
	clj -M:run-worker

debug-api:
	clj -X:dbg flow-storm.api/cli-run :instrument-ns '#{"aws." "api."}' :fn-symb 'api.main/-main' :fn-args '[]'

debug-worker:
	clj -X:dbg flow-storm.api/cli-run :instrument-ns '#{"worker." "aws." "api."}' :fn-symb 'worker.main/-main' :fn-args '[]'

lint:
	clj -M:lint --lint src/clj src/cljc
