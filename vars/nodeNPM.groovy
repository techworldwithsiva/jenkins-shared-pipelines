#!groovy

def call(Map configMap){
    echo "Pipeline started just now"
    def pomMap = [:]
    pipeline{
        agent any
        environment{
            AWS_ACCOUNT_ID="752692907119"
            REGION="ap-south-1"
            IMAGE_REPO="hello-world-web"
            IMAGE_TAG="latest"
            REPO_URI="${AWS_ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com/${IMAGE_REPO}:${IMAGE_TAG}"
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
        withAWS(credentials: 'My AWS Credentials', region: "${env.AWS_REGION}") {
                    sh """
                        aws eks update-kubeconfig --name my-eks-cluster
                        kubectl apply -f my-deployment.yaml
                    """
                }
        stage('Image push to ECR'){
            steps{
                withAWS(credentials: 'aws-auth', region: "${REGION}") {
                    sh """
                        aws ecr get-login-password --region ${REGION} | docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com
                    docker tag ${IMAGE_REPO}:latest  ${REPO_URI}:latest
                    docker push ${REPO_URI}:latest
                    """
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