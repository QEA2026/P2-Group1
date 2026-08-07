pipeline {
    agent any

    stages {
        stage('Deploy Containers') {
            steps {
                sh '''
                    docker compose down --remove-orphans || true
                    docker compose up -d employee-app manager-app selenium
                '''
            }
        }

        stage('Build Employee Docker Image') {
            steps {
                sh 'docker build -t employee-app -f python/Dockerfile .'
            }
        }

        stage('Build Manager Docker Image') {
            steps {
                sh 'docker build -t manager-app -f expense-app-managers/Dockerfile .'
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
                dir('expense-app-managers') {
                    sh ' docker run --rm manager-app mvn clean test -Dtest=JDBCManagerDAOTest'
                }
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
}