pipeline {
    agent any

    stages {

        stage('Build Test Employee Image') {
            steps {
                sh ' docker build -t employee-test -f python/Dockerfile .'
            }
        }

        stage('Build Test Manager Image') {
            steps {
                sh ' docker build -t manager-test -f expense-app-managers/Dockerfile.test .'
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

        stage('Build Deployment Employee Image') {
            steps {
                sh 'docker build -t employee-app -f python/Dockerfile .'
            }
        }

        stage('Build Deployment Manager Image') {
            steps {
                sh 'docker build -t manager-app -f expense-app-managers/Dockerfile .'
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

                    until curl -s http://selenium:4444/status | grep -q '"ready":true'; do
                        sleep 5
                    done

                    echo "Selenium is ready!"
                '''
            }
        }

        stage('Employee E2E Tests') {
            steps {
                sh '''
                    docker compose run e2e-tests
                    docker compose down
                '''
            }
        }
    }

    post {
        always {
            sh '''
                docker compose down --remove-orphans || true
            '''
        }
    }
}