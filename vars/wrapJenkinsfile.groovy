import ru.inie.tool.PipelineWrapper

def call(def jenkinsfileContext) {
    PipelineWrapper wrapper = new PipelineWrapper(jenkinsfileContext)
    wrapper.execute()
}