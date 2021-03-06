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
	String label
	def project
	def token
	def ticketRegex = ~/\d{6,}/
	def numberRegex = ~/\d{6,}/
	def ptUrl = "https://www.pivotaltracker.com/services/v5/projects/"
	def headers = ["Story ID", "Type", "Name", "Description", "URL"]
	def propertyMap = ["Story ID":"id", "Type":"story_type", "Name":"name", "Description":"description", "URL":"url" ]

	/**
	 * Generates a release notes report containing all
	 * @param file the file to output to
	 */
	void generateReleaseNotes(file) {

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

		writeReleaseNoteReport(stories, printer)
	}

	void generateLabelReport(file) {
		log.info("(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)")
		log.info("Validating release epic for label ${label} for branch ${branch} between ${start} and ${end}.")
		log.info("(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)")

		def logs = getLogs()
		def tickets = getTickets(logs)

		log.info("Found ${tickets.size()} tickets: ${tickets} in logs.")

		def labelStories = getStoriesForLabel()

		log.info("Retrieved ${labelStories.size()} stories.")
		if (labelStories.size() == 0) {
			throw new Exception("Couldn't find any stories")
		}
		log.debug(labelStories)
		def storiesToRemove = []
		labelStories.each {story ->
			if (tickets.contains(story.id.toString())) {
				tickets.remove(story.id.toString())
				storiesToRemove << story
			}
		}
		labelStories.removeAll(storiesToRemove)

		def commitStories = getStories(tickets)

		FileWriter writer = new FileWriter(file)
		IndentPrinter printer = new IndentPrinter(writer)

		def builder = new MarkupBuilder(printer)
		builder.h1 label
		writeLabelReport("Unlabeled on Branch", commitStories, builder)
		writeLabelReport("Missing From Branch", labelStories, builder)
	}

	void generateProgressReport(file) {
		log.info("(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)")
		log.info("Generating progress report for release label ${label}.")
		log.info("(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)(_Y_)")

		def labelStories = getStoriesForLabel()

		Set uniqueLabels = new HashSet()

		labelStories.each {
			uniqueLabels.addAll(it.labels*.name)
		}

		def epics = uniqueLabels.collect { epic ->
			def epicStories = labelStories.findAll {story ->
				story.labels.any {it.name == epic}
			}
			def row = [:]
			def epicFeatures = epicStories.findAll { story ->
				(story.story_type == "feature")
			}
			row.name = epic
			row.delivered = epicFeatures.findAll {story ->
				["accepted", "delivered"].contains(story.current_state)
			}.sum {story ->
				story?.estimate?:0
			}
			row.total = epicStories.sum {story ->
				story?.estimate ?: 0
			}
			row.deliveredBugs = epicStories.count { story ->
				(story.story_type == "bug") &&
						["accepted", "delivered"].contains(story.current_state)
			}
			row.bugsTotal = epicStories.count { story ->
				(story.story_type == "bug")
			}
			return row
		}

		FileWriter writer = new FileWriter(file)
		IndentPrinter printer = new IndentPrinter(writer)

		def builder = new MarkupBuilder(printer)
		writeProgressReport("Progress Report ${label}", epics, builder)

	}

	def getTickets(logs) {
		def tickets = logs.findAll(ticketRegex)
		tickets = tickets.unique().collect { it.find(numberRegex) }
		return tickets
	}

	def getLogs() {
		return "git log ${start}..${end} --format=%b".execute().text
	}

	def getStoriesForLabel() {
		def restClient = new RESTClient("${ptUrl}${project}/", ContentType.JSON)
		restClient.headers.'X-TrackerToken' = token
		def stories = []
		try {
			int offset = 0
			boolean more = true
			while (more) {
				def response = restClient.get(path: "stories",
											  query: [with_label: label, offset: offset, envelope: true]).data
				def pagination = response.pagination
				stories.addAll(response.data)
				offset += pagination.limit as Integer
				more = offset < pagination.total
			}
			return stories
		} catch (ex) {
			log.error("Failed to get stories for label ${label}")
			log.error(ex, ex)
			throw (ex)
		}
		return []
	}

	def getStories(tickets) {
		def restClient = new RESTClient("${ptUrl}${project}/stories/", ContentType.JSON)
		restClient.headers.'X-TrackerToken' = token

		def stories = []

		tickets.each {
			def story
			try {
				story = getStory(restClient, it)
				log.info("Retrieving story ${it}")

			} catch (ex) {
				log.info("Failed to retrieve story for story ${it}")
				log.error(ex, ex)
				story = [id:it, name: "ERROR COULD NOT FIND story ${it} IN PT PROJECT ${project}", labels:[], url:"https://www.pivotaltracker.com/story/show/${it}"]
			}
			stories << story

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

	void writeReleaseNoteReport(stories, printer) {

		new MarkupBuilder(printer).table {
			thead {
				tr {
					headers.each {h ->
						th h
					}
					if (label) {
						th label
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
						if (label) {
							def labels = story.labels*.name.sort()
							String result = labels.contains(label)?"YES":"NO"
							td result
						}
					}
				}
			}
		}
	}

	void writeLabelReport(tableCaption, stories, builder) {
		builder.table {
			thead {
				caption tableCaption
				tr {
					th "Name"
					th "URL"
					th "Labels"
				}
			}
			tbody {
				stories.each { story ->
					tr {
						td story.get("name")
						td {
							a(href: story.get("url"),  story.get("url"))
						}
						td story.labels*.name?.join(", ")
					}
				}
			}
		}
	}

	void writeProgressReport(tableCaption, epics, builder) {
		builder.table {
			thead {
				caption tableCaption
				tr {
					th "Name"
					th "Delivered"
					th "Total"
					th "Delivered Bugs"
					th "Total Bugs"
				}
			}
			tbody {
				epics.each { story ->
					tr {
						td story.get("name")
						td story.get("delivered")
						td story.get("total")
						td story.get("deliveredBugs")
						td story.get("bugsTotal")
					}
				}
			}
		}
	}

	void setActionFilter(String action) {
		if (action != "") {
			ticketRegex = ~/${action} #${numberRegex}/
		} else {
			ticketRegex = ~/${numberRegex}/
			// we aren't using the hash so our search includes potential typos
		}
	}


}
