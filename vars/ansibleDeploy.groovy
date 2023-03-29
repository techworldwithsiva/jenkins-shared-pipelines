#!groovy

def call(Map configMap){
    echo "Pipeline started"
    def groupID, artifactID, version, repoURL
    pipeline{
        parameters{
            string(name: 'groupID', description: 'Please provide grpup ID')
            string(name: 'artifactID', description: 'Please provide artifact ID')
            string(name: 'version', description: 'Please provide version')
            string(name: 'repoURL', description: 'Please provide Repo URL')
            
        }
        agent {label 'JENKINS_SLAVE'}
        environment{
            AWS_ACCOUNT_ID="084767242532"
            REGION="ap-southeast-1"
            IMAGE_REPO="echo-project"
            IMAGE_TAG="latest"
            REPO_URI="${AWS_ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com/${IMAGE_REPO}"
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

            stage('set params'){
                steps{
                    script{
                        groupID = params.groupID
                        artifactID = params.artifactID
                        version = params.version
                        repoURL = params.repoURL
                    }
                }
            }

            stage('Run Deploy Playbook'){
                steps{
                    script{
                        echo "ansible ping"
                        withCredentials([usernamePassword(credentialsId: 'ansible-creds', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                            withCredentials([usernamePassword(credentialsId: 'nexus-creds', usernameVariable: 'nexusUsername', passwordVariable: 'nexusPassword')]) {
                                sh '''
                                    
                                    ansible-playbook playbook.yaml --user=${USERNAME} -e "ansible_ssh_pass=${PASSWORD}" -e "ansible_become_pass=${PASSWORD}" -e "groupID=$groupID" -e "artifactID=$artifactID" -e "version=$version" -e "repoURL=$repoURL" -e "nexusUsername=$nexusUsername" -e "nexusPassword=$nexusPassword"
                                '''
                            }
                        }
                        
                    }
                }
            }
            
        }
    }
}