#!/usr/bin/env groovy
// workaround to have groovy scripts in github that can be used without 
// the sandbox.
//
// Needs to be copied in Jenkins as a Pipeline Script 
// with 'Use Groovy Sandbox' unticked

node () {
    checkout([$class: 'GitSCM', 
              branches: [[name: '*/master']], 
              doGenerateSubmoduleConfigurations: false, 
              extensions: [[$class: 'CleanCheckout']], 
              submoduleCfg: [], 
              userRemoteConfigs: [[url: 'https://github.com/tarekziade/jenkins_scripts.git']]]
    )
    pipeline = load 'pipeline.groovy'
    failures = pipeline.testProject(PROJECT_NAME)
    stage('Ship it!') {
        if (failures == 0) {
            sh 'exit 0'
        } else {
            sh 'exit 1'
        }
    }
}

