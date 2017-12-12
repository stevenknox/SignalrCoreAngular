node {
    try {
        build()
        BuildImage()
        PushImage()
        Deploy()
        notify("Build Success")
    } catch(err) {
        notify("Build Failure ${err}")
        currentBuild.result = 'FAILURE'
    }
}

def build(){
    stage 'Checkout'
    git 'https://github.com/stevenknox/SignalrCoreAngular.git'
    dir('src')
    {
        stage 'Install NPM Packages'
        sh 'npm install'
        
        stage 'Publish Asp.Net Core App'
        sh 'dotnet publish signalrcorespa.csproj -c Release -o ./obj/Docker/publish'    
    }
    stage 'Run Server Tests'
    sh 'dotnet test ./test/test.csproj'    
}

def BuildImage(){
    stage('Build image')
    /* This builds the actual image; synonymous to
         * docker build on the command line */
    dir('src')
    {
        app = docker.build("stevenknox/signalrcorespa")
    }
}

def PushImage(){
    stage('Push image')
    /* Finally, we'll push the image with two tags:
     * First, the incremental build number from Jenkins
     * Second, the 'latest' tag.
     * Pushing multiple tags is cheap, as all the layers are reused. */
    dir('src')
    {
        docker.withRegistry('https://registry.hub.docker.com', 'docker-hub-credentials') {
            app.push("${env.BUILD_NUMBER}")
            app.push("latest")
        }
    }
}

def Deploy(){
    stage('Deploy to Kubernetes Cluster')
    dir('src')
    {
        kubernetesDeploy configs: 'k8s-*.yml', kubeConfig: [path: '../../kubeconfig'], secretName: '', ssh: [sshCredentialsId: '*', sshServer: ''], textCredentials: [certificateAuthorityData: '', clientCertificateData: '', clientKeyData: '', serverUrl: 'https://']
    }
}

def notify(status){
    emailext (
      to: "steven@whole.school",
      subject: "${status}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
      body: """<p>${status}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
        <p>Check console output at <a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a></p>""",
    )
}