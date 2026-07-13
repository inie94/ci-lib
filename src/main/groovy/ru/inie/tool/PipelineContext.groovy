package ru.inie.tool

interface PipelineContext {
    void stage(String name, Closure body)
    void echo(String message)
    void sh(String command)
    void error(String message)
    void checkout(Object scm)
    void ansiColor(String name, Closure body)
    void timestamps(Closure body)
    void node(Closure body)
    Map getEnv()
    Object getScm()
}