pipeline {
    agent any

    stages {
        stage('Build Employee Docker Image') {
            steps {
                sh 'docker build -t employee-app -f python/Dockerfile .'
            }
        }

        stage('Employee Unit Tests') {
            steps {
                dir('python') {
                    // sh 'docker run --rm employee-app ls -l /app'
                    sh ' docker run --rm employee-app pytest tests/unit'
                }
            }
        }
    }
}