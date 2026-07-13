package ru.inie.tool

import spock.lang.Specification
import spock.lang.Unroll

class PipelineWrapperTest extends Specification {

    PipelineContext context
    ChangeDetector changeDetector
    PipelineStageExecutor stageExecutor
    PipelineWrapper wrapper

    def setup() {
        context = Mock(PipelineContext)
        changeDetector = Mock(ChangeDetector)
        stageExecutor = Mock(PipelineStageExecutor)

        setupContextMock()

        wrapper = new PipelineWrapper(context, changeDetector, stageExecutor)
    }

    private void setupContextMock() {
        // Методы с замыканиями — выполняем их
        context.ansiColor(_, _) >> { String name, Closure body -> body.call() }
        context.timestamps(_) >> { Closure body -> body.call() }
        context.node(_) >> { Closure body -> body.call() }
        context.stage(_, _) >> { String name, Closure body -> body.call() }

        // Свойства
        context.getScm() >> new Object()
    }

    def "should execute commit pipeline when change type is COMMIT"() {
        given:
        context.getEnv() >> [
                'GIT_COMMIT': 'abc123',
                'BRANCH_NAME': 'feature/test'
        ]

        when:
        wrapper.executePipeline()

        then:
        1 * changeDetector.detect(_) >> ChangeDetector.ChangeType.COMMIT
        1 * stageExecutor.executeCommitStages()
        0 * stageExecutor.executePRStages(_)
        0 * stageExecutor.executeDeployToDev()
    }

    def "should execute PR pipeline when change type is PULL_REQUEST"() {
        given:
        def env = [
                'CHANGE_ID': '42',
                'CHANGE_BRANCH': 'feature/test',
                'CHANGE_TARGET': 'main'
        ]
        context.getEnv() >> env

        and:
        def prInfo = [
                'id': '42',
                'sourceBranch': 'feature/test',
                'targetBranch': 'main'
        ]

        when:
        wrapper.executePipeline()

        then:
        1 * changeDetector.detect(_) >> ChangeDetector.ChangeType.PULL_REQUEST
        1 * changeDetector.getPullRequestInfo(_) >> prInfo
        1 * stageExecutor.executePRStages(prInfo)
        0 * stageExecutor.executeCommitStages()
    }

    def "should handle TAG change type"() {
        given:
        context.getEnv() >> [
                'TAG_NAME': 'v1.0.0',
                'GIT_COMMIT': 'abc123'
        ]

        when:
        wrapper.executePipeline()

        then:
        1 * changeDetector.detect(_) >> ChangeDetector.ChangeType.TAG
        1 * stageExecutor.executeTagStages()
        0 * stageExecutor.executeCommitStages()
        0 * stageExecutor.executePRStages(_)
    }

    @Unroll
    def "should deploy to DEV for main branches: #branch"() {
        given:
        // Возвращаем Map с BRANCH_NAME = конкретное значение для каждого теста
        context.getEnv() >> [ 'GIT_COMMIT': 'abc123', 'BRANCH_NAME': branch ]

        when:
        wrapper.executePipeline()

        then:
        1 * changeDetector.detect(_) >> ChangeDetector.ChangeType.COMMIT
        1 * stageExecutor.executeCommitStages()
        1 * stageExecutor.executeDeployToDev()
        0 * stageExecutor.executePRStages(_)

        where:
        branch << ['main', 'master', 'develop']
    }

    def "should NOT deploy to DEV for feature branch"() {
        given:
        context.getEnv() >> [
                'GIT_COMMIT': 'abc123',
                'BRANCH_NAME': 'feature/test'
        ]

        when:
        wrapper.executePipeline()

        then:
        1 * changeDetector.detect(_) >> ChangeDetector.ChangeType.COMMIT
        1 * stageExecutor.executeCommitStages()
        0 * stageExecutor.executeDeployToDev()
    }

    def "should throw error for UNKNOWN change type"() {
        given:
        context.getEnv() >> [:]

        when:
        wrapper.executePipeline()

        then:
        1 * changeDetector.detect(_) >> ChangeDetector.ChangeType.UNKNOWN
        1 * context.error(_)
        0 * stageExecutor.executeCommitStages()
        0 * stageExecutor.executePRStages(_)
    }
}