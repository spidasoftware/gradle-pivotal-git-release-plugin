package com.spidasoftware.releasenotes

import groovy.util.logging.Log4j
import groovy.xml.MarkupBuilder
import groovyx.net.http.ContentType
import groovyx.net.http.RESTClient

/**
 * (_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)
 *
 * Class for creating release notes from pivotal.
 * Finds all stories mentioned between start and end (end defaults to HEAD)
 * and produces a report of their information from pivotal tracker
 * User: mford
 * Date: 2/11/14
 * Time: 1:58 PM
 * Copyright SPIDAWeb
 * (_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)
 */
@Log4j
class ReleaseNotes {

	String start = "HEAD"
	String end = "HEAD"
	def project
	def token
	def ticketRegex = ~/#\d{6,}/
	def numberRegex = ~/\d{6,}/
	def ptUrl = "https://www.pivotaltracker.com/services/v5/projects/"
	def headers = ["Story ID", "Type", "Name", "Description", "URL"]
	def propertyMap = ["Story ID":"id", "Type":"story_type", "Name":"name", "Description":"description", "URL":"url" ]

	File generateReleaseNotes(file) {

		log.info("(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)")
		log.info("Generating release notes for ${branch} between ${start} and ${end}.")
		log.info("(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)")

		def logs = getLogs()
		def tickets = getTickets(logs)

		log.info("Found ${tickets.size()} tickets: ${tickets}")

		def stories = getStories(tickets)
		log.info("Retrieved ${stories.size()} stories.")
		log.trace(stories)

		FileWriter writer = new FileWriter(file)
		IndentPrinter printer = new IndentPrinter(writer)

		writeStories(stories, printer)

		return file
	}

	def getTickets(logs) {
		def tickets = logs.findAll(ticketRegex)
		tickets = tickets.unique().collect { it.find(numberRegex) }
		return tickets
	}

	def getLogs() {
		return "git log ${start}..${end} --format=%b".execute().text
	}

	def getStories(tickets) {
		def restClient = new RESTClient("${ptUrl}${project}/stories/", ContentType.JSON)
		restClient.headers.'X-TrackerToken' = token

		def stories = []

		tickets.each {
			try {
				def story = getStory(restClient, it)
				log.info("Retrieving story ${it}")

				stories << story
			} catch (ex) {
				log.error(ex, ex)
			}
		}

		return stories
	}

	def getStory(restClient, storyId) {
		log.trace "getting story ${storyId}"
		def story = restClient.get(path: storyId)
		return story.data
	}

	def getBranch() {
		return "git rev-parse --abbrev-ref HEAD".execute().text.trim()
	}

	void writeStories(stories, printer) {

		new MarkupBuilder(printer).table {
			thead {
				tr {
					headers.each {h ->
						th h
					}
				}
			}
			tbody {

				stories.each { story ->
					tr {
						headers.each {h->
							String field = propertyMap.get(h)
							if (story.get(field).toString().startsWith("http")) {
								td {
									a(href: story.get(field),  story.get(field))
								}
							} else {
								td story.get(field)
							}
						}
					}
				}
			}
		}
	}

	void setActionFilter(String action) {
		if (action != "") {
			ticketRegex = ~/${action} #${numberRegex}/
		} else {
			ticketRegex = ~/#${numberRegex}/
		}
	}


}
