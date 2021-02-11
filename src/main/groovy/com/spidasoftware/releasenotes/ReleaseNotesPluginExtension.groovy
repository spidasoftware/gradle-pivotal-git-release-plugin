package com.spidasoftware.releasenotes

import groovy.util.logging.Log4j

/**
*
* User: mford
* Date: 2/12/14
* Time: 12:15 PM
* Copyright SPIDAWeb
*/
@Log4j
class ReleaseNotesPluginExtension {
	String start = "HEAD"
	String end = "HEAD"
	String project
	String token
	String fileName = "./build/release-notes.html"
	String labelReportFileName = "./build/release-label-report.html"
	String progressReportFileName = "./build/release-progress-report.html"
	String actionFilter = ""
	String label = ""
}
