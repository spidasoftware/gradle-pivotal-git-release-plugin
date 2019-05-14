package com.spidasoftware.releasenotes

import org.gradle.api.Plugin
import org.gradle.api.Project
/**
*
* User: mford
* Date: 2/12/14
* Time: 10:35 AM
* Copyright SPIDAWeb
*/

class ReleaseNotesPlugin implements Plugin<Project>{

	@Override
	void apply(Project project) {
		project.extensions.create('releaseNotes', ReleaseNotesPluginExtension)

		project.task('releaseNotes') << {

			def startHash = project.hasProperty('start')? project.properties.get('start') : project.extensions.releaseNotes.start
			def endHash = project.hasProperty('end')? project.properties.get('end') : project.extensions.releaseNotes.end
			def action = project.hasProperty('actionFilter')? project.properties.get('actionFilter').trim() : project.extensions.releaseNotes.actionFilter
			def label = project.hasProperty('label')? project.properties.get('label') : project.extensions.releaseNotes.label

			if (!project.extensions.releaseNotes.project) {
				throw new IllegalArgumentException("project must be specified in releaseNotes configuration")
			}
			if (!project.extensions.releaseNotes.token) {
				throw new IllegalArgumentException("token must be specified in releaseNotes configuration")
			}

			println "Creating Release Notes from ${startHash}..${endHash}"
			ReleaseNotes notes = new ReleaseNotes(start: startHash,
				end: endHash,
				project: project.extensions.releaseNotes.project,
				token: project.extensions.releaseNotes.token,
				actionFilter: action,
			    label:label)

			def output = project.file(project.extensions.releaseNotes.fileName)
			notes.generateReleaseNotes(output)
			println "Created release notes at ${output.toURL().toString()}"
		}

		project.task('releaseLabelReport') << {

			def startHash = project.hasProperty('start')? project.properties.get('start') : project.extensions.releaseNotes.start
			def endHash = project.hasProperty('end')? project.properties.get('end') : project.extensions.releaseNotes.end
			def action = project.hasProperty('actionFilter')? project.properties.get('actionFilter').trim() : project.extensions.releaseNotes.actionFilter
			def label = project.hasProperty('label')? project.properties.get('label') : project.extensions.releaseNotes.label

			if (!project.extensions.releaseNotes.project) {
				throw new IllegalArgumentException("project must be specified in releaseNotes configuration")
			}
			if (!project.extensions.releaseNotes.token) {
				throw new IllegalArgumentException("token must be specified in releaseNotes configuration")
			}

			ReleaseNotes notes = new ReleaseNotes(start: startHash,
												  end: endHash,
												  project: project.extensions.releaseNotes.project,
												  token: project.extensions.releaseNotes.token,
												  actionFilter: action,
												  label:label)

			def output = project.file(project.extensions.releaseNotes.labelReportFileName)
			println project.extensions.releaseNotes.labelReportFileName
			notes.generateLabelReport(output)
			println "Created label validation report at ${output.toURL().toString()}"
		}

	}

}
