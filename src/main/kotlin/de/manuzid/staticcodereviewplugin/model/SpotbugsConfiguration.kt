package de.manuzid.staticcodereviewplugin.model

data class SpotbugsConfiguration(val artifactId: String, val filePaths: List<String>, val priorityThresholdLevel: Int,
                                 val absolutePath: String, val applicationSourcePath: String, val compiledClassPath: String)
