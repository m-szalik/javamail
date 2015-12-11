all:
		mvn clean install
fast:
		mvn -Dmaven.test.skip=true install
test:
		mvn test
clean:
		mvn clean
