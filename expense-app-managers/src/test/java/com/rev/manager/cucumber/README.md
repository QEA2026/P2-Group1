(1st Terminal)
1. mvn package -DskipTests
2. mvn exec:java -Dexec.mainClass="com.rev.manager.Main" -Dexec.args="testDatabase.db"
(2nd Terminal)
3.  mvn clean test