
# Gradle Version Tool

[![Build Status](https://travis-ci.com/netmikey/gradle-version-tool.svg?branch=master)](https://travis-ci.com/netmikey/gradle-version-tool)

A tool for managing the Gradle versions of multiple projects.


## Purpose

If you use Gradle as your build system, a best practice is using the [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html). On a side note, you might want to check out my [Gradle Wrapper delegator](https://github.com/netmikey/gradle-wrapper-delegator). If you start using Gradle and the Gradle wrapper on more and more of your projects, you might end up with a dozen or more projects, each of which bringing its own Gradle wrapper. While being autonomous is a great thing, keeping track of which project uses which Gradle version and having to maintain all of these projects and their Gradle versions by hand can get tedious, error-prone and time-consuming.

The Gradle Version Tool combines file system scanning, the [Gradle Tooling API](https://docs.gradle.org/current/userguide/embedding.html) and [JGit](https://www.eclipse.org/jgit/) to find and list all your Gradle Wrapper-enabled projects along with their current Gradle version and the ability to auto-upgrade them.


## Features

* Scan through all directories to find Gradle Wrapper enabled projects. Projects that don't use the Gradle wrapper will be ignored.
* Automatially retrieves information about the latest published Gradle version from GitHub
* Analyze projects to find which Gradle version is used, whether it is up-to-date, and whether it is under Git version control
* Automatically upgrade projects to the latest Gradle version
  * Gradle version upgrade will only be attempted it the project meets all criteria:
    * If using Git, the working directory must be clean.
    * If using Git and a remote, the current branch must be in sync with the remote repository.
    * If `major-upgrades` is set to `false`, upgrades across major Gradle versions (e.g. from 4.10.2 to 5.x) will not be attempted.
  * To validate a successful Gradle migration, the tool runs the project's build before and after the upgrade. The build must work both times for the upgrade to be considered successful.
  * Automatically commit and push changes


## Usage

The general syntax of the tool is:

    gradle-version-tool [options]

Options are specified in the form: `--[option]=[value]`

Available options are:

Option | Optional | Default | Description
------ | -------- | ------- | ------------
`dir` | Yes | current working directory | The top directory from which the tool starts scanning down to find Gradle projects.
`action` | Yes | list | One of: <ul><li>`list` only list found projects and their Gradle version</li><li>`upgrade` upgrade found projects if applicable</li></ul>
`dry-run` | Yes | false | Scan and evaluate only, don't do anything on storage.<br/><br/> Only applicable in `upgrade` mode.
`major-upgrades` | Yes | false | Whether upgrades across Gradle major versions should be attempted (e.g. 4.10.2 to 5.x).<br/><br/> Only applicable in `upgrade` mode.
`check-tasks` | Yes | clean, build, assemble | The comma-separated list of Gradle tasks to be used to check that the build works. They will be used before and after upgrading Gradle.<br/><br/> Only applicable in `upgrade` mode.
`commit` | Yes | true | Whether the upgraded Gradle wrapper files should be committed to Git.<br/><br/>Only applicable in `upgrade` mode, for projects under Git version control for which an upgrade was successful.
`push` | Yes | true | Whether the commit of the new Gradle wrapper files should be pushed to the Git remote.<br/><br/>Only applicable in `upgrade` mode, when `commit` is `true`, for projects under Git version control for which an upgrade was successful.

### Examples

* List all projects with their Gradle version

        c:\my-projects\> gradle-version-tool

* See which projects would be upgraded to the latest Gradle version

        c:\my-projects\> gradle-version-tool --action=upgrade --dry-run=true

* Upgrade a single project to the latest Gradle version, do a commit but don't push to the remote repository

        c:\my-projects\> gradle-version-tool \
            --dir=c:\my-projects\someproject\ \
            --action=upgrade \
            --push=false
