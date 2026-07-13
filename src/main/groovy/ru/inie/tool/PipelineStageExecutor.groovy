package ru.inie.tool

class PipelineStageExecutor {
    private def script
    
    PipelineStageExecutor(def script) {
        this.script = script
    }
    
    void executeCommitStages() {
        buildProject()
        runUnitTests()
        runStaticAnalysis()
    }
    
    void executePRStages(Map prInfo) {
        buildProject()
        runUnitTests()
        runIntegrationTests()
        runPRSonarAnalysis(prInfo)
        checkMergeConflicts(prInfo)
    }
    
    void executeTagStages() {
        buildProject()
        runUnitTests()
        runIntegrationTests()
        runFullSonarAnalysis()
        generateReleaseNotes()
    }
    
    void executeDeployToDev() {
        script.stage('Deploy to DEV') {
            script.echo 'Deploying to development environment'
            script.sh '''
                echo "Deploying application to DEV..."
                # Здесь команды деплоя
            '''
        }
    }
    
    private void buildProject() {
        script.stage('Build') {
            script.echo 'Building project...'
            script.sh '''
                echo "Running Maven build..."
                mvn clean compile
            '''
        }
    }
    
    private void runUnitTests() {
        script.stage('Unit Tests') {
            script.echo 'Running unit tests...'
            script.sh '''
                echo "Running unit tests..."
                mvn test
            '''
        }
    }
    
    private void runIntegrationTests() {
        script.stage('Integration Tests') {
            script.echo 'Running integration tests...'
            script.sh '''
                echo "Running integration tests..."
                mvn verify -DskipUnitTests=true
            '''
        }
    }
    
    private void runStaticAnalysis() {
        script.stage('Static Analysis') {
            script.echo 'Running static analysis...'
            script.sh '''
                echo "Running SonarQube analysis..."
                mvn sonar:sonar
            '''
        }
    }
    
    private void runPRSonarAnalysis(Map prInfo) {
        script.stage('SonarQube PR Analysis') {
            script.echo 'Running SonarQube PR decoration...'
            script.sh """
                echo "Running SonarQube PR analysis..."
                mvn sonar:sonar \\
                    -Dsonar.pullrequest.branch=${prInfo.sourceBranch} \\
                    -Dsonar.pullrequest.base=${prInfo.targetBranch} \\
                    -Dsonar.pullrequest.key=${prInfo.id}
            """
        }
    }
    
    private void runFullSonarAnalysis() {
        script.stage('Full SonarQube Analysis') {
            script.echo 'Running full SonarQube analysis...'
            script.sh '''
                echo "Running full SonarQube analysis..."
                mvn sonar:sonar -Dsonar.qualitygate.wait=true
            '''
        }
    }
    
    private void checkMergeConflicts(Map prInfo) {
        script.stage('Check Merge Conflicts') {
            script.echo 'Checking for merge conflicts...'
            script.sh """
                echo "Checking conflicts with target branch: ${prInfo.targetBranch}"
                # Проверка на конфликты слияния
                git fetch origin ${prInfo.targetBranch}
                git merge-base --is-ancestor HEAD origin/${prInfo.targetBranch} || \\
                    echo "WARNING: Branch is behind target"
            """
        }
    }
    
    private void generateReleaseNotes() {
        script.stage('Generate Release Notes') {
            script.echo 'Generating release notes...'
            script.sh '''
                echo "Generating release notes..."
                git log --pretty=format:"- %s" $(git describe --tags --abbrev=0 HEAD^)..HEAD > release-notes.txt
            '''
        }
    }
}