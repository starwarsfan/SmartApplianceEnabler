library identifier: 'icheko-jenkins-shared-lib@master',
        retriever: modernSCM([
                $class: 'GitSCMSource',
                id    : '13ebda5f-2be5-4751-83d4-4d4500603cc5',
                remote: 'https://github.com/camueller/jenkins-shared-lib',
                traits: [[$class: 'jenkins.plugins.git.traits.BranchDiscoveryTrait']]
        ]) _

pipeline {
    agent {
        label 'buildx'
    }

    parameters {
        booleanParam(name: 'DOCKER_PUSH', defaultValue: false, description: 'Push docker image to Dockerhub?')
        booleanParam(name: 'BETA_RELEASE', defaultValue: false, description: 'Is this a beta release?')
        string(name: 'DOCKER_IMAGE_NAME', defaultValue: 'avanux/smartapplianceenabler', description: 'Default name of Docker image')
    }

    environment {
        VERSION = readMavenPom().getVersion()
        BROWSERSTACK_USERNAME = credentials('BROWSERSTACK_USERNAME')
        BROWSERSTACK_ACCESS_KEY = credentials('BROWSERSTACK_ACCESS_KEY')
        DOCKER_TAG = "${env.BETA_RELEASE == "true" ? "beta" : "latest"}"
    }

    stages {
//        stage('Checkout') {
//            steps {
//                scmSkip(deleteBuild: false, skipPattern: '.*\\[ci skip\\].*')
//            }
//        }
        stage('Build') {
            steps {
                sh(
                    script: """
                        mvn clean -B -Pweb  
                        mvn package -B -Pweb
                    """
                )
            }
        }
        stage('Dockerize') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                    sh(
                        script: """
                            echo $PASSWORD | docker login --username $USERNAME --password-stdin
                            ./buildImages.sh -7 -8 -t ${DOCKER_IMAGE_NAME}:ci -v ${VERSION}
                        """
                    )
                }
            }
        }
        stage('Chrome') {
            steps {
                sh(
                    script: """
                        docker stop sae || true
                        docker volume rm -f sae
                        docker volume create sae
                        docker run -d --rm -v sae:/opt/sae/data -p 8081:8080 --name sae ${DOCKER_IMAGE_NAME}:ci
                        cd src/test/angular
                        npm i
                        npm run test:chrome
                    """
                )
            }
        }
        stage('Firefox') {
            steps {
                sh(
                    script: """
                        docker stop sae || true
                        docker volume rm -f sae
                        docker volume create sae
                        docker run -d --rm -v sae:/opt/sae/data -p 8081:8080 --name sae ${DOCKER_IMAGE_NAME}:ci
                        cd src/test/angular
                        npm run test:firefox
                    """
                )
            }
        }
        stage('Safari') {
            steps {
                sh(
                    script: """
                        docker stop sae || true
                        docker volume rm -f sae
                        docker volume create sae
                        docker run -d --rm -v sae:/opt/sae/data -p 8081:8080 --name sae ${DOCKER_IMAGE_NAME}:ci
                        cd src/test/angular
                        npm run test:safari
                    """
                )
            }
        }
        stage('Stop') {
            steps {
                sh "docker stop sae || true"
            }
        }
        stage('Publish') {
            when {
                expression { params.DOCKER_PUSH }
            }
            steps {
                sh(
                    script: """
                        ./buildImages.sh -t ${DOCKER_IMAGE_NAME}:${DOCKER_TAG} -p
                    """
                )
            }
        }
    }
}
