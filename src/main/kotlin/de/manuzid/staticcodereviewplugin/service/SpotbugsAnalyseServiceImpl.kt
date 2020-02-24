/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.manuzid.staticcodereviewplugin.service

import de.manuzid.staticcodereviewplugin.model.Issue
import de.manuzid.staticcodereviewplugin.model.SpotbugsConfiguration
import edu.umd.cs.findbugs.*
import edu.umd.cs.findbugs.config.UserPreferences
import org.apache.maven.plugin.MojoExecutionException
import org.slf4j.LoggerFactory
import java.io.IOException

class SpotbugsAnalyseServiceImpl(private val spotbugsConfiguration: SpotbugsConfiguration) : AnalyseService {

    val logger = LoggerFactory.getLogger(SpotbugsAnalyseServiceImpl::class.java)

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


    override fun analyse(): AnalyseService {
        try {
            engine.execute()
        } catch (e: IOException) {
            logger.error("Analysis failed.", e)
        } catch (e: InterruptedException) {
            logger.error("Analysis was interrupted.", e)
        }

        if (bugReporter.queuedErrors.isNotEmpty()) {
            bugReporter.reportQueuedErrors()
            logger.error("Analysis failed. Check stderr for detail.")
        }

        return this
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
                    .filter { it.endsWith(".java") }
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
