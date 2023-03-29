#!groovy

def call(Map configMap){
    echo "Pipeline started"
    def pomMap = [:]
    pipeline{
        agent {label 'JENKINS_SLAVE'}
        environment{
            AWS_ACCOUNT_ID="084767242532"
            REGION="ap-southeast-1"
        }
        options{
            timeout(time: 1, unit: 'HOURS')
            disableConcurrentBuilds()
            skipDefaultCheckout()
            timestamps()
        }
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
                        
                        withCredentials([usernamePassword(credentialsId: 'terraform-creds', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                            
                        sh '''
                        export AWS_ACCESS_KEY_ID="${USERNAME}"
                        export AWS_SECRET_ACCESS_KEY="${PASSWORD}"
                        export AWS_DEFAULT_REGION=ap-southeast-1
                        '''
                        }
                    }
                }
            }
            stage('Create Cluster'){
                steps{
                    script{
                        sh '''
                        eksctl get cluster --config-file cluster.yaml 
                        if [ $? -ne 0 ]
                        then
                            eksctl create cluster --config-file cluster.yaml
                        else
                            echo "cluster already created"
                        fi
                        '''
                    }
                }
            }
            stage('Cluster AutoScaler'){
                steps{
                    script{
                        sh '''
                        kubectl apply -f cluster-auto-scaler.yaml
                        '''
                    }
                }
            }
            stage('EFS-Setup'){
                steps{
                    script{
                        sh '''
                        kubectl apply -f efs-provisioner.yaml
                        kubectl apply -f storage-class.yaml
                        '''
                    }
                }
            }
            stage('EFS-DB'){
                steps{
                    script{
                        sh '''
                        kubectl apply -f mysql-efs.yaml
                        '''
                    }
                }
            }

        }
    }
}