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

package de.manuzid.staticcodereviewplugin.mojo

import de.manuzid.staticcodereviewplugin.model.Issue
import de.manuzid.staticcodereviewplugin.model.SpotbugsConfiguration
import de.manuzid.staticcodereviewplugin.service.AnalyseService
import de.manuzid.staticcodereviewplugin.service.SpotbugsAnalyseServiceImpl
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter

@Mojo(name = "report-spotbugs", defaultPhase = LifecyclePhase.VERIFY)
class SpotBugsReportMojo : AbstractReportMojo() {

    @Parameter(property = "priorityThresholdLevel", defaultValue = "3")
    private var priorityThresholdLevel: Int = 3

    override fun getAnalyzer(): String = "spotbugs"

    override fun getIssuesFromAnalyzer(affectedFilePaths: List<String>): List<Issue> =
            spotbugsAnalyseServiceImpl(affectedFilePaths).analyse().getReportedIssues()

    private fun spotbugsAnalyseServiceImpl(filePaths: List<String>): AnalyseService {
        val spotbugsConfiguration = SpotbugsConfiguration(project.artifactId, filePaths, priorityThresholdLevel,
                project.build.directory, applicationSourcePath.removeSurrounding("/"),
                compiledClassPath.removeSurrounding("/"))
        return SpotbugsAnalyseServiceImpl(spotbugsConfiguration)
    }

    private fun String.removeSurrounding(sign: String): String = this.removePrefix(sign).removeSuffix(sign)

}
