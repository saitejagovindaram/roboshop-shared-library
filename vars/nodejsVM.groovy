def call(Map configMap){
    pipeline {
        agent {
            node {
                label 'agent-1'
            }
        }
        // agent any

        environment {
            packageVersion = ''
            NEXUS_VERSION = "nexus3"
            NEXUS_PROTOCOL = "http"
            // NEXUS_URL = "172.31.20.124:8081"
            NEXUS_URL = "${params.NexusURL}"
            NEXUS_REPOSITORY = "catalogue"
            NEXUS_CREDENTIAL_ID = 'nexus-auth'
        }

        options {
            timeout(time: 1, unit: 'HOURS')
            disableConcurrentBuilds()
            ansiColor('xterm')
        }
        parameters{
            string(name: 'NexusURL', defaultValue: '', description: 'what is the Nexux IP address')
            booleanParam(name: 'Deploy', defaultValue: false, description: 'Want to deploy?')
        }
        stages {
            stage('Get the version') {
                steps {
                    script {
                        def packageJson = readJSON file: 'package.json'
                        packageVersion = packageJson.version
                        echo "application version: ${packageVersion}"
                    }
                }
            }
            stage('install dependencies') {
                steps {
                    sh '''
                        npm install
                    '''
                }
            }
            stage('Unit Test'){
                steps {
                    sh 'echo Unit tests will run here'
                }
            }
            stage('Sonar Scanning'){
                steps{
                    sh 'sonar-scanner'
                }
            }
            stage('Build'){
                steps {
                    sh '''
                        ls -ltr
                        zip -q -r catalogue.zip ./*
                        ls -ltr
                    '''
                }
            }

            stage('Publish artifact'){
                steps {
                    script {
                        nexusArtifactUploader(
                                nexusVersion: "${NEXUS_VERSION}",
                                protocol: NEXUS_PROTOCOL,
                                nexusUrl: NEXUS_URL,
                                groupId: 'com.roboshop',
                                version: "${packageVersion}",
                                repository: NEXUS_REPOSITORY,
                                credentialsId: NEXUS_CREDENTIAL_ID,
                                artifacts: [
                                    // Artifact generated such as .jar, .ear and .war files.
                                    [artifactId: 'catalogue',
                                    classifier: '',
                                    file: 'catalogue.zip',
                                    type: 'zip'],

                                    // // Lets upload the pom.xml file for additional information for Transitive dependencies
                                    // [artifactId: pom.artifactId,
                                    // classifier: '',
                                    // file: "pom.xml",
                                    // type: "pom"]
                                ]
                        );
                    }
                }
            }
            stage('Deploy'){
                when {
                    expression { 
                        params.deploy == true
                    }
                }   
                steps{
                    // build job: 'roboshop-dev/catalogue-cd', parameters: [
                    //     string(name: 'version', value: "${packageVersion}"), 
                    //     string(name: 'environment', value: 'dev')]

                    build(job: 'roboshop-dev/catalogue-cd', parameters: [  // In groovy we can call a method like above as well, both will work.
                        string(name: 'version', value: "${packageVersion}"), 
                        string(name: 'environment', value: 'dev')]);
                }
            }
        }
        post {
            always{
                echo 'I will run always'
                deleteDir()
            }
        }
    }
}