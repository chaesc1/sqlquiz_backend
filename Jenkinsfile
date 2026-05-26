// Jenkins declarative pipeline.
//
// 트리거: main 브랜치 push (multibranch). develop 으로 들어오는 PR 은 test 까지만, main 은 docker push 포함.
// 필수 자격증명 (Jenkins Credentials):
//   - dockerhub-creds : Docker Hub username/password (id 가능)
//   - sqlquiz-prod-env: 운영 env 파일 (Secret file) — 본 파이프라인에선 build 후 deploy 단계에서 사용
//
// 학습 노트:
//   - Spring Boot 빌드는 ./gradlew bootJar 로 충분 (test 는 별도 stage)
//   - DOCKER_BUILDKIT=1 활성화로 멀티스테이지 캐시 활용
//   - main 외 브랜치에서는 push 를 skip — 안전 가드

pipeline {
    agent any

    options {
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '5'))
    }

    environment {
        IMAGE_NAME = 'chaesc1/sqlquiz-backend'
        IMAGE_TAG  = "${env.GIT_COMMIT?.take(8) ?: env.BUILD_NUMBER}"
        DOCKER_BUILDKIT = '1'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Test') {
            steps {
                sh './gradlew --no-daemon clean test'
            }
            post {
                always {
                    junit testResults: '**/build/test-results/test/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Build Jar') {
            steps {
                sh './gradlew --no-daemon bootJar -x test'
                archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
            }
        }

        stage('Build Image') {
            when { branch 'main' }
            steps {
                sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} -t ${IMAGE_NAME}:latest ."
            }
        }

        stage('Push Image') {
            when { branch 'main' }
            steps {
                withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-creds',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS')]) {
                    sh '''
                      echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                      docker push ${IMAGE_NAME}:${IMAGE_TAG}
                      docker push ${IMAGE_NAME}:latest
                    '''
                }
            }
        }

        // 실제 배포 stage 는 호스트 환경에 따라 달라지므로 자리만 잡아둠.
        // ex) ssh 로 prod 서버 접속 → docker pull + restart, 혹은 docker-compose pull && up -d
        stage('Deploy (placeholder)') {
            when { branch 'main' }
            steps {
                echo "TODO: deploy ${IMAGE_NAME}:${IMAGE_TAG} to prod server"
            }
        }
    }

    post {
        success { echo "✅ Build #${env.BUILD_NUMBER} succeeded — ${IMAGE_NAME}:${IMAGE_TAG}" }
        failure { echo "❌ Build #${env.BUILD_NUMBER} failed" }
        always  { cleanWs() }
    }
}
