.PHONY: run cljs init test build

run:
	clojure -m flexblock.core

cljs:
	npx shadow-cljs compile app

init:
	npm install

test:
	clojure -A:test --reporter kaocha.report/documentation

build:
	npx shadow-cljs release app
	clojure -A:depstar:prod
