/**@
    Deploy/push Docker image to Container Registry

    parameters :
        credentialsId : Credentials id for login to Container Registry
        cloudType : Cloud provider name (Alibaba Cloud, Google Cloud, AWS, AWS CLI) -> AWS use file credentials, AWS CLI use access key&secret
        registryURL : Container Registry endpoint URL
        imageName : Docker image name to push to container registry
        regionId : (AWS CLI) AWS Region ID
        BranchName : Versi Git Branch 

    credentialsId :
        Alibaba Cloud : username&password
        Google Cloud : file
        AWS : file
        AWS CLI : username&password
*/
def call(Map config = [:]) {
    config.imageName = config.imageName.replaceAll("/null/", "/").replaceAll("//", "/").replaceFirst(":/+", "://")
    config.BranchName = config.BranchName.replace("/", "-")
    
    if (config.cloudType == "Alibaba Cloud") {
        docker.withRegistry("${config.registryURL}", "${config.credentialsId}") {
            dockerImageRemote = docker.build "${config.imageName}:${config.BranchName}"
            dockerImageRemote.push()
            dockerImageRemote.push("latest")
        }
    } else if (config.cloudType == "Google Cloud") {
        withCredentials([file(credentialsId: "${config.credentialsId}", variable: 'FILE')]) {
            sh "cat $FILE | docker login -u _json_key --password-stdin ${config.registryURL}"
        }
        dockerImageRemote = docker.build "${config.imageName}:${config.BranchName}"
        dockerImageRemote.push()
        dockerImageRemote.push("latest")
    } else if (config.cloudType == "AWS") {
        withCredentials([file(credentialsId: "${config.credentialsId}", variable: 'FILE')]) {
            sh "cat $FILE | docker login --username AWS --password-stdin ${config.registryURL}"
        }
        dockerImageRemote = docker.build "${config.imageName}:${config.BranchName}"
        dockerImageRemote.push()
        dockerImageRemote.push("latest")
    } else if (config.cloudType == "AWS CLI") {
        echo "AWS CLI"
        if (!env.AWS_DEFAULT_REGION) {
            env.AWS_DEFAULT_REGION = config.regionId
        }
        withCredentials([usernamePassword(credentialsId: "${config.credentialsId}", passwordVariable: 'SECRET', usernameVariable: 'KEY')]) {
            sh "aws configure set aws_access_key_id $KEY"
            sh "aws configure set aws_secret_access_key $SECRET"
            sh "aws ecr get-login-password > ~/aws_creds.txt"
            sh "cat ~/aws_creds.txt | docker login --username AWS --password-stdin ${config.registryURL}"
        }
        dockerImageRemote = docker.build "${config.imageName}:${config.BranchName}"
        dockerImageRemote.push()
        dockerImageRemote.push("latest")
    } else if (config.cloudType == "Azure") {
        echo "Azure Provider is under maintenance or unavailable"
    }
    else if (config.cloudType == "Local Registry") {
            dockerImageRemote = docker.build "${config.imageName}:${config.BranchName}"
            dockerImageRemote.push()
            dockerImageRemote.push("latest")
    }
}
