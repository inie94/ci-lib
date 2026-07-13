package ru.inie.tool

import spock.lang.Specification

class PipelineStageExecutorTest extends Specification {
    
    def script
    PipelineStageExecutor executor
    
    def setup() {
        script = Mock(Script)
        executor = new PipelineStageExecutor(script)
    }
    
    def "executeCommitStages should run build, tests and static analysis"() {
        when:
        executor.executeCommitStages()
        
        then:
        3 * script.stage(_, _)
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
        5 * script.stage(_, _) // Build, Unit Tests, Integration Tests, SonarQube, Merge Conflicts
    }
    
    def "executeTagStages should include deployment preparation"() {
        when:
        executor.executeTagStages()
        
        then:
        5 * script.stage(_, _) // Build, Unit Tests, Integration Tests, SonarQube, Release Notes
    }
    
    def "executeDeployToDev should deploy to development"() {
        when:
        executor.executeDeployToDev()
        
        then:
        1 * script.stage('Deploy to DEV', _)
        1 * script.sh(_)
        1 * script.echo(_)
    }
}