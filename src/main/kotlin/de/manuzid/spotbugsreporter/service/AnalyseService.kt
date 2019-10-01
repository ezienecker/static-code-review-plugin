package de.manuzid.spotbugsreporter.service

import de.manuzid.spotbugsreporter.model.Issue
import de.manuzid.spotbugsreporter.model.SpotbugsConfiguration
import edu.umd.cs.findbugs.*
import edu.umd.cs.findbugs.config.UserPreferences
import org.apache.maven.plugin.MojoExecutionException
import java.io.IOException

interface AnalyseService {

    fun analyse()

    fun getReportedIssues(): List<Issue>

}

class SpotbugsAnalyseServiceImpl(private val spotbugsConfiguration: SpotbugsConfiguration) : AnalyseService {

    private val engine = FindBugs2()
    private val bugReporter: BugCollectionBugReporter

    init {
        DetectorFactoryCollection.resetInstance(DetectorFactoryCollection())

        val project = createProject(spotbugsConfiguration)
        engine.project = project

        val detectorFactoryCollection = DetectorFactoryCollection.instance()
        engine.setDetectorFactoryCollection(detectorFactoryCollection)

        bugReporter = BugCollectionBugReporter(project)
        bugReporter.setPriorityThreshold(getPriorityThreshold(spotbugsConfiguration.priorityThresholdLevel))
        bugReporter.setRankThreshold(BugRanker.VISIBLE_RANK_MAX)

        val userPreferences = userPreferences()
        val checkCallsFactory = detectorFactoryCollection.getFactory("CheckCalls")
        userPreferences.enableDetector(checkCallsFactory, false)

        engine.bugReporter = bugReporter
        engine.userPreferences = userPreferences
    }


    override fun analyse() {
        try {
            engine.execute()
        } catch (e: IOException) {
            throw MojoExecutionException("Analysis failed.", e)
        } catch (e: InterruptedException) {
            throw MojoExecutionException("Analysis was interrupted.", e)
        }

        if (bugReporter.queuedErrors.isNotEmpty()) {
            bugReporter.reportQueuedErrors()
            throw MojoExecutionException("Analysis failed. Check stderr for detail.")
        }
    }

    override fun getReportedIssues(): List<Issue> =
            bugReporter.bugCollection.map { transformBugInstanceToIssueList(it) }.flatten().toList()

    private fun createProject(spotbugsConfiguration: SpotbugsConfiguration): Project =
            Project().apply {
                projectName = spotbugsConfiguration.artifactId
                addAllFiles(transformApplicationPathsToAbsoluteClassPaths(spotbugsConfiguration.filePaths))
            }

    private fun transformApplicationPathsToAbsoluteClassPaths(affectedFilePaths: List<String>): List<String> =
            affectedFilePaths
                    .map { path -> path.replace(spotbugsConfiguration.applicationSourcePath, spotbugsConfiguration.absolutePath + '/' + spotbugsConfiguration.compiledClassPath) }
                    .map { path -> path.replace(".java", ".class") }
                    .toList()

    private fun getPriorityThreshold(priorityThresholdLevel: Int): Int =
            when (priorityThresholdLevel) {
                1 -> Priorities.HIGH_PRIORITY
                2 -> Priorities.NORMAL_PRIORITY
                3 -> Priorities.LOW_PRIORITY
                4 -> Priorities.EXP_PRIORITY
                5 -> Priorities.IGNORE_PRIORITY
                else -> throw IllegalArgumentException("There is no priority threshold level for value: " +
                        "$priorityThresholdLevel")
            }

    private fun userPreferences(): UserPreferences =
            UserPreferences.createDefaultUserPreferences()
                    .apply {
                        filterSettings.clearAllCategories()
                        enableAllDetectors(true)
                    }

    private fun transformBugInstanceToIssueList(bugInstance: BugInstance): List<Issue> {
        val sourcePath = getFullPathOfSourceFile(bugInstance)
        val message = bugInstance.toString()

        return bugInstance.annotations.map { getSourceLineNumber(it) }
                .filter { it != -1 }
                .map { Issue(sourcePath, it, message) }
                .toList()
    }

    private fun getSourceLineNumber(annotation: BugAnnotation): Int =
            when (annotation) {
                is SourceLineAnnotation -> annotation.endLine
                else -> -1
            }

    private fun Project.addAllFiles(filePaths: List<String>): Unit =
            filePaths.forEach { addFile(it) }

    private fun getFullPathOfSourceFile(bugInstance: BugInstance): String =
            spotbugsConfiguration.applicationSourcePath + '/' + bugInstance.primaryClass.className.replace('.', '/').plus(".java")

}
