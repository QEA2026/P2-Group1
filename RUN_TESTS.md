## Running Manager App Tests:

First run the Manager App:
1: Go to directory expense-app-managers
2: mvn clean package -DskipTests
3: mvn exec:java "-Dexec.mainClass=com.rev.manager.Main" "-Dexec.args=testDatabase.db"

Open a new terminal
1: Go to directory expense-app-managers
2: mvn clean test (to run all tests)
3: mvn allure:report
4: mvn allure:serve

## Running Employee App Tests:

For the unit tests:

1: There are 3 testing python files with their names ending with: business_logic.py, happyPath_sadPath.py, and mocking.py

   To run each of them as follows:
   pytest python/tests/unit

   a: pytest python/tests/employeeApp_business_logic.py -v

   b: pytest python/tests/employeeApp_happyPath_sadPath.py -v

   c: pytest python/tests/employeeApp_mocking.py -v

For the test reports: 

1: There are 3 testing python files with their names ending with: business_logic.py, happyPath_sadPath.py, and mocking.py

   To generate test results for each of them as follows:

   a: pytest python/tests/employeeApp_business_logic.py --alluredir=allure-results

   b: pytest python/tests/employeeApp_happyPath_sadPath.py --alluredir=allure-results

   c: pytest python/tests/employeeApp_mocking.py --alluredir=allure-results

3: For each of the same testing python files:

   To generate test reports:

   a: pytest python/tests/employeeApp_business_logic.py --html=report.html --self-contained-html

   b: pytest python/tests/employeeApp_happyPath_sadPath.py --html=report.html --self-contained-html

   c: pytest python/tests/employeeApp_mocking.py --html=report.html --self-contained-html

## Running API Tests:

API tests were done through postman manually. For more info on APIs, visit API_Documentation.txt. (From the project root folder, Go to P1_Group1\python\api>)

## Running E2E Employee App Test:

1: From the project root folder, Go to P1_Group1\python\api>
2: Python app.py (Runs the (Flask) python backend)
3: New terminal - From the Project root folder, Go to P1_Group1\python\api\features then, type 'behave' (Should run the e2e testing simulation on browser.)

## Running Performance Tests:
1: Run Manager App in one terminal
 - Go to directory expense-app-managers
 - mvn exec:java -D exec.mainClass="com.rev.manager.Main"
2: Run Employee App
 - Go to main directory (P1)
 - python /api/app.py
3: Open JMeter
4: Load test plan "Expense App Tests"

To execute regular usage test, run test with only the Regular Usage thread enabled. View results under summary report.
To execute Limit test, run with only Limit thread enabled.
To execute Spike test, run with BOTH Regular Usage and Spike threads enabled.