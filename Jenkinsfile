pipeline {
    agent any

    stages {
        stage('Deploy Containers') {
            steps {
                sh '''
                    docker compose down --remove-orphans || true
                    docker compose up -d employee-app selenium
                '''
            }
        }

        stage('Build Employee Docker Image') {
            steps {
                sh 'docker build -t employee-app -f python/Dockerfile .'
            }
        }

        stage('Employee Unit Tests') {
            steps {
                dir('python') {
                    sh ' docker run --rm employee-app pytest python/tests/unit'
                }
            }
        }

        stage('Employee E2E Tests') {
            steps {
                sh '''
                    docker compose up -d employee-app selenium
                    docker compose run e2e-tests
                    docker compose down
                '''
            }
        }
    }
}