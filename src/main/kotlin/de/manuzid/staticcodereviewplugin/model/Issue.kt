package de.manuzid.staticcodereviewplugin.model

data class Issue(val sourceFilePath: String, val line: Int, val message: String)
