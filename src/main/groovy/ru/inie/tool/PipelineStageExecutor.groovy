package ru.inie.tool

class PipelineStageExecutor {
    private PipelineContext context

    PipelineStageExecutor(PipelineContext context) {
        this.context = context
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
        context.stage('Deploy to DEV') {
            context.echo 'Deploying to development environment'
            context.sh '''
                echo "Deploying application to DEV..."
                # Здесь команды деплоя
            '''
        }
    }
    
    private void buildProject() {
        context.stage('Build') {
            context.echo 'Building project...'
            context.sh '''
                echo "Running Maven build..."
                mvn clean compile
            '''
        }
    }
    
    private void runUnitTests() {
        context.stage('Unit Tests') {
            context.echo 'Running unit tests...'
            context.sh '''
                echo "Running unit tests..."
                mvn test
            '''
        }
    }
    
    private void runIntegrationTests() {
        context.stage('Integration Tests') {
            context.echo 'Running integration tests...'
            context.sh '''
                echo "Running integration tests..."
                mvn verify -DskipUnitTests=true
            '''
        }
    }
    
    private void runStaticAnalysis() {
        context.stage('Static Analysis') {
            context.echo 'Running static analysis...'
            context.sh '''
                echo "Running SonarQube analysis..."
                mvn sonar:sonar
            '''
        }
    }
    
    private void runPRSonarAnalysis(Map prInfo) {
        context.stage('SonarQube PR Analysis') {
            context.echo 'Running SonarQube PR decoration...'
            context.sh """
                echo "Running SonarQube PR analysis..."
                mvn sonar:sonar \\
                    -Dsonar.pullrequest.branch=${prInfo.sourceBranch} \\
                    -Dsonar.pullrequest.base=${prInfo.targetBranch} \\
                    -Dsonar.pullrequest.key=${prInfo.id}
            """
        }
    }
    
    private void runFullSonarAnalysis() {
        context.stage('Full SonarQube Analysis') {
            context.echo 'Running full SonarQube analysis...'
            context.sh '''
                echo "Running full SonarQube analysis..."
                mvn sonar:sonar -Dsonar.qualitygate.wait=true
            '''
        }
    }
    
    private void checkMergeConflicts(Map prInfo) {
        context.stage('Check Merge Conflicts') {
            context.echo 'Checking for merge conflicts...'
            context.sh """
                echo "Checking conflicts with target branch: ${prInfo.targetBranch}"
                # Проверка на конфликты слияния
                git fetch origin ${prInfo.targetBranch}
                git merge-base --is-ancestor HEAD origin/${prInfo.targetBranch} || \\
                    echo "WARNING: Branch is behind target"
            """
        }
    }
    
    private void generateReleaseNotes() {
        context.stage('Generate Release Notes') {
            context.echo 'Generating release notes...'
            context.sh '''
                echo "Generating release notes..."
                git log --pretty=format:"- %s" $(git describe --tags --abbrev=0 HEAD^)..HEAD > release-notes.txt
            '''
        }
    }
}