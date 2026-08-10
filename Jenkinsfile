pipeline {
    agent any

    stages {

        stage('Build Docker Images') {
            steps {
                sh 'docker compose build'
            }
        }

        stage('Employee Unit Tests') {
            steps {
                dir('python') {
                    sh ' docker run --rm employee-app pytest python/tests/unit'
                }
            }
        } 

        stage('Manager Unit Tests') {
            steps {
                sh ' docker run --rm manager-test mvn test -Dtest=JDBCManagerDAOTest'
            }
        }

        stage('Create Test Database Volume') {
            steps {
                sh '''
                    docker volume inspect vol-test-database >/dev/null 2>&1 || \
                    docker volume create vol-test-database
                '''
            }
        }
        
        stage('Start Test Servers') {
            steps {
                sh ' docker compose up -d employee-app-test manager-app-test'
            }
        }

        stage('Employee API Tests') {
            steps {
                sh ' docker compose run --rm api-tests mvn test -Dtest=test_emp_endpoints'
            }
        }

        stage('Manager API Tests') {
            steps {
                sh ' docker compose run --rm api-tests mvn test -Dtest=test_manager_endpoints'
            }
        }

        stage('Deploy Containers') {
            steps {
                sh '''
                    docker compose down --remove-orphans || true
                    docker compose up -d employee-app manager-app selenium
                '''
            }
        }

        stage('Wait for Selenium') {
            steps {
                sh '''
                    echo "Waiting for Selenium..."

                    until curl -sf host.docker.internal:4444/status > /dev/null; do
                        echo "Selenium not ready yet..."
                        sleep 5
                    done

                    echo "Selenium is ready!"
                '''
            }
        }

        stage('Employee E2E Tests') {
            steps {
                sh ' docker compose run --rm e2e-tests'
            }
        }
    }

    post {
        always {
            sh '''
                docker compose down --remove-orphans || true
            '''
        }

        success {
            echo 'Pipeline completed successfully!'
        }

        failure {
            echo 'Pipeline failed!'
        }
    }
}