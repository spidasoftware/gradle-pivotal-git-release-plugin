package com.spidasoftware.releasenotes

import groovy.json.JsonSlurper
import groovy.xml.MarkupBuilder

/**
 *
 * User: mford
 * Date: 2/12/14
 * Time: 10:33 AM
 * Copyright SPIDAWeb
 */
class ReleaseNotesTest extends GroovyTestCase {

	void testGetTickets() {
		ReleaseNotes notes = new ReleaseNotes()

		def logs = """[Delivers #1234567] Did some stuff
and some other stuff [Finishes #1234568]
and some other stuff [Delivers #1234568]
456
#45 and junk
[Delivers 1234569]
"""
		assertEquals("Should have found two tickets", [1234567, 1234568, 1234569].toString(), notes.getTickets(logs).toString())
	}

	void testWriteStories() {
		
		String jsonString = '''
{                                                                                                                                        
  "project_id": 147173,                                                                                                                  
  "description": "myDescription",
  "story_type": "bug",                                                                                                                   
  "created_at": "2013-12-17T23:21:05Z",                                                                                                  
  "updated_at": "2014-01-08T14:15:36Z",                                                                                                  
  "owner_ids": [                                                                                                                         
    174681                                                                                                                               
  ],                                                                                                                                     
  "id": 62676970,                                                                                                                        
"name": "myName",
		"current_state": "accepted",
		"labels": [
		  {
		    "project_id": 147173,
		    "id": 1921759,
		    "name": "calc",
		    "kind": "label"
		  },
		  {
		    "project_id": 147173,
		    "created_at": "2013-08-12T20:18:26Z",
		    "updated_at": "2013-08-12T20:18:26Z",
		    "id": 6293370,
		    "name": "engine-5.0",
		    "kind": "label"
		  }
		],
		"requested_by_id": 174913,
		"kind": "story",
		"accepted_at": "2014-01-08T14:15:33Z",
		"owned_by_id": 174681,
		"url": "http://www.pivotaltracker.com/story/show/62676970"
}
		'''

		def story = new JsonSlurper().parseText(jsonString)

		String expected = """<table>
  <thead>
    <tr>
      <th>Story ID</th>
      <th>Type</th>
      <th>Name</th>
      <th>Description</th>
      <th>URL</th>
      <th>engine-5.0</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>62676970</td>
      <td>bug</td>
      <td>myName</td>
      <td>myDescription</td>
      <td>
        <a href='http://www.pivotaltracker.com/story/show/62676970'>http://www.pivotaltracker.com/story/show/62676970</a>
      </td>
      <td>YES</td>
    </tr>
  </tbody>
</table>"""
		StringWriter writer = new StringWriter()
		IndentPrinter printer = new IndentPrinter(writer)

		ReleaseNotes notes = new ReleaseNotes(label:"engine-5.0")
		def stories = [story]
		notes.writeReleaseNoteReport(stories, printer)

		assertEquals("Should match output html", expected, writer.toString())

	}

	void testGenerateLabelReport() {
		def stories = [
		        [name:"123",
				url: "http://myurl.com",
				labels:[[name:"joe"]]],
				[name: "456",
				url: "http://yoururl.com"]
		]
		StringWriter writer = new StringWriter()
		IndentPrinter printer = new IndentPrinter(writer)

		ReleaseNotes notes = new ReleaseNotes()
		def builder = new MarkupBuilder(printer)
		notes.writeLabelReport("My caption", stories, builder)

		String expected =  """<table>
  <thead>
    <caption>My caption</caption>
    <tr>
      <th>Name</th>
      <th>URL</th>
      <th>Labels</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>123</td>
      <td>
        <a href='http://myurl.com'>http://myurl.com</a>
      </td>
      <td>joe</td>
    </tr>
    <tr>
      <td>456</td>
      <td>
        <a href='http://yoururl.com'>http://yoururl.com</a>
      </td>
      <td />
    </tr>
  </tbody>
</table>"""

		assertEquals("Should match output html", expected, writer.toString())

	}
}
