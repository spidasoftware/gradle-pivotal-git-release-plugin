Release Notes
=============================
A gradle plugin using git and pivotal tracker integration to tell you what story are on your branch.
-----------------------------

The release notes plugin adds a task that searches your current branch's git log for pivotal tracker integration symbols
like [Delivers #ticketNumber] in between the start and end commits specified. For every unique story id it finds, it
retrieves the story details from tracker, and puts a summary of them in a table. This way you can know exactly which
pivotal stories were resolved by the current build.

### Tasks:

    releaseNotes - generates release-notes.html in your build folder. lists all
    releaseLabelReport - generates a report of how well the labeled stories in pivotal tracker match your commits

### How to include

Add to your build.gradle file

    apply plugin: 'releaseNotes'

...

    buildscript {
      dependencies {
        classpath 'com.spidasoftware:releaseNotes:0.1-SNAPSHOT'
      }
    }

### Minimum required configuration:

    releaseNotes {
      project = "put your pivotal tracker project id here"
      token = "put your pivotal tracker user api token here"
    }

### Default configuration:

    releaseNotes {
      start = "HEAD"  // The start  git commit. Can be any commit name recognized by git.
      end = "HEAD"  // The end git commit.  Can be any commit name recognized by git.
      fileName = "./build/release-notes.html" // The output file produced
      labelReportFileName = "./build/release-label-report.html" // the output file for the releaseLabelReport task
      project = null  // The tracker project id. Required in config.
      token = null    // The tracker user api token. Required in config.
      actionFilter = "" // Optional filter on pt integration commands. "Delivers" "Finishes" etc.
      label = "" //  release label to check against
    }

### Command line options

    -Pstart=[start commit]
    -Pend=[end commit]
    -PactionFilter=[action filter string]
