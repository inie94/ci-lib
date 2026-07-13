package ru.inie.tool

class PipelineWrapper {
    private def script
    private ChangeDetector changeDetector
    private PipelineStageExecutor stageExecutor
    
    PipelineWrapper(def script) {
        this.script = script
        this.changeDetector = new ChangeDetector()
        this.stageExecutor = new PipelineStageExecutor(script)
    }
    
    // Конструктор для тестирования с внедрением зависимостей
    PipelineWrapper(def script, ChangeDetector changeDetector, PipelineStageExecutor stageExecutor) {
        this.script = script
        this.changeDetector = changeDetector
        this.stageExecutor = stageExecutor
    }
    
    void execute() {
        script.ansiColor('xterm') {
            script.timestamps {
                script.node {
                    executePipeline()
                }
            }
        }
    }
    
    private void executePipeline() {
        script.stage('Checkout') {
            script.checkout script.scm
        }
        
        ChangeDetector.ChangeType changeType = changeDetector.detect(script.env)
        
        script.stage('Detect Change Type') {
            script.echo "Detected change type: ${changeType}"
        }
        
        switch (changeType) {
            case ChangeDetector.ChangeType.PULL_REQUEST:
                handlePullRequest()
                break
            case ChangeDetector.ChangeType.COMMIT:
                handleCommit()
                break
            case ChangeDetector.ChangeType.TAG:
                handleTag()
                break
            default:
                script.error "Unknown change type. Cannot proceed."
        }
    }
    
    private void handleCommit() {
        script.echo "Processing commit to branch: ${script.env.BRANCH_NAME}"
        script.echo "Commit SHA: ${script.env.GIT_COMMIT}"
        
        stageExecutor.executeCommitStages()
        
        if (isMainBranch()) {
            stageExecutor.executeDeployToDev()
        }
    }
    
    private void handlePullRequest() {
        Map prInfo = changeDetector.getPullRequestInfo(script.env)
        
        script.echo "Processing Pull Request:"
        script.echo "  ID: ${prInfo.id}"
        script.echo "  Source: ${prInfo.sourceBranch}"
        script.echo "  Target: ${prInfo.targetBranch}"
        
        stageExecutor.executePRStages(prInfo)
    }
    
    private void handleTag() {
        script.echo "Processing tag: ${script.env.TAG_NAME}"
        
        stageExecutor.executeTagStages()
        
        script.stage('Deploy to Production') {
            script.echo "Deploying tagged version to production"
            script.sh "echo 'Deploying version ${script.env.TAG_NAME}'"
        }
    }
    
    private boolean isMainBranch() {
        def branch = script.env.BRANCH_NAME
        return branch == 'main' || branch == 'master' || branch == 'develop'
    }
}