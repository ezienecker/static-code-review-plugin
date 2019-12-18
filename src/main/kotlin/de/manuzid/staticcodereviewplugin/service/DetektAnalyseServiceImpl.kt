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

import de.manuzid.staticcodereviewplugin.model.DetektConfiguration
import de.manuzid.staticcodereviewplugin.model.Issue
import io.gitlab.arturbosch.detekt.api.Detektion
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.api.RuleSetId
import io.gitlab.arturbosch.detekt.core.DetektFacade
import io.gitlab.arturbosch.detekt.core.ProcessingSettings
import java.nio.file.Paths

class DetektAnalyseServiceImpl(detektConfiguration: DetektConfiguration) : AnalyseService {

    private val detektFacade: DetektFacade
    private lateinit var detektion: Detektion

    init {
        val filePaths = detektConfiguration.filePaths.map { Paths.get(it) }.filter { !it.endsWith(".kt") }.toList()
        val processingSettings = ProcessingSettings(filePaths)
        detektFacade = DetektFacade.create(processingSettings)
    }

    override fun analyse(): AnalyseService {
        detektion = detektFacade.run()
        return this
    }

    override fun getReportedIssues(): List<Issue> =
            detektion.findings.map { transformFindingsToIssueList(it) }.flatten().toList()

    private fun transformFindingsToIssueList(findings: Map.Entry<RuleSetId, List<Finding>>): List<Issue> =
            findings.value.map {
                Issue(it.entity.location.file, it.entity.location.source.line, it.messageOrDescription())
            }.toList()

}
