// src/main/groovy/ru/inie/tool/PipelineWrapper.groovy
package ru.inie.tool

class PipelineWrapper {
    private PipelineContext context
    private ChangeDetector changeDetector
    private PipelineStageExecutor stageExecutor

    PipelineWrapper(PipelineContext context, ChangeDetector changeDetector,
                    PipelineStageExecutor stageExecutor) {
        this.context = context
        this.changeDetector = changeDetector
        this.stageExecutor = stageExecutor
    }

    void execute() {
        context.ansiColor('xterm') {
            context.timestamps {
                context.node {
                    executePipeline()
                }
            }
        }
    }

    void executePipeline() {
        context.stage('Checkout') {
            context.checkout(context.scm)  // ← теперь scm доступен
        }

        ChangeDetector.ChangeType changeType = changeDetector.detect(context.getEnv())

        context.stage('Detect Change Type') {
            context.echo("Detected change type: ${changeType}")
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
                context.error("Unknown change type. Cannot proceed.")
        }
    }

    private void handleCommit() {
        context.echo("Processing commit to branch: ${context.getEnv().BRANCH_NAME}")
        context.echo("Commit SHA: ${context.getEnv().GIT_COMMIT}")
        stageExecutor.executeCommitStages()

        if (isMainBranch()) {
            stageExecutor.executeDeployToDev()
        }
    }

    private void handlePullRequest() {
        Map prInfo = changeDetector.getPullRequestInfo(context.getEnv())
        context.echo("Processing Pull Request:")
        context.echo("  ID: ${prInfo.id}")
        context.echo("  Source: ${prInfo.sourceBranch}")
        context.echo("  Target: ${prInfo.targetBranch}")
        stageExecutor.executePRStages(prInfo)
    }

    private void handleTag() {
        context.echo("Processing tag: ${context.getEnv().TAG_NAME}")
        stageExecutor.executeTagStages()
        context.stage('Deploy to Production') {
            context.echo("Deploying tagged version to production")
            context.sh("echo 'Deploying version ${context.getEnv().TAG_NAME}'")
        }
    }

    private boolean isMainBranch() {
        def branch = context.getEnv().BRANCH_NAME
        return branch == 'main' || branch == 'master' || branch == 'develop'
    }
}