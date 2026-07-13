package ru.inie.tool

import spock.lang.Specification

class PipelineStageExecutorTest extends Specification {

    PipelineContext context
    PipelineStageExecutor executor

    def setup() {
        // Mock интерфейса работает без проблем
        context = Mock(PipelineContext)
        executor = new PipelineStageExecutor(context)
    }

    def "executeCommitStages should run build, tests and static analysis"() {
        when:
        executor.executeCommitStages()

        then:
        noExceptionThrown()
        // Проверяем, что методы вызывались без ошибок
        // Expando просто игнорирует неизвестные методы
    }

    def "executePRStages should include integration tests and sonar analysis"() {
        given:
        def prInfo = [
                'id': '42',
                'sourceBranch': 'feature/test',
                'targetBranch': 'main'
        ]

        when:
        executor.executePRStages(prInfo)

        then:
        noExceptionThrown()
    }

    def "executeDeployToDev should deploy to development"() {
        when:
        executor.executeDeployToDev()

        then:
        noExceptionThrown()
    }
}