#!groovy

def call(Map configMap){
    echo "Pipeline started just now"
    def pomMap = [:]
    pipeline{
        agent none
        environment{
            AWS_ACCOUNT_ID="084767242532"
            REGION="ap-southeast-1"
            IMAGE_REPO="student-ui"
            IMAGE_TAG="latest"
            REPO_URI="${AWS_ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com/${IMAGE_REPO}"
            CI=false
        }
        /* options{
            timeout(time: 1, unit: 'HOURS')
            disableConcurrentBuilds()
            skipDefaultCheckout()
            timestamps()
        } */
        stages{
            stage('Clone'){
                steps{
                    script{
                        echo "Clone started"
                        gitInfo = checkout scm
                        
                    }
                }
            }
            stage('Build'){
                steps{
                    script{
                        sh '''
                            
                            npm install
                            npm run build
                            ls -ltr
                        '''
                    }
                }
            }

        stage('Docker build'){
            steps{
                script{
                    sh "docker build -t ${IMAGE_REPO}:latest . "
                }
            }
        }
        stage('Image push to ECR'){
            steps{
                script{
                    sh '''
                    aws ecr get-login-password --region ${REGION} | docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com
                    docker tag ${IMAGE_REPO}:latest  ${REPO_URI}:latest
                    docker push ${REPO_URI}:latest
                    '''
                }
            }
        }
        stage('Deploy'){
            steps{
                script{
                    sh '''
                        kubectl apply -f manifest.yaml
                    '''
                }
            }
        }
        }
    }
}