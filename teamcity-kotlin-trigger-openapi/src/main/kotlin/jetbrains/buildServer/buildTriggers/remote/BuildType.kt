package jetbrains.buildServer.buildTriggers.remote

import java.util.*

class BuildType(
    val id: String,
    val isInQueue: Boolean,
    val isPaused: Boolean,
    val project: Project,
    val history: List<FinishedBuild>,
    val lastChangesStartedBuild: Build?,
    val lastChangesSuccessfullyFinished: Build?,
    val lastChangesFinished: Build?,
    val buildParameters: Map<String, String>,
    val parameters: Map<String, String>,
    val tags: List<String>
)

class Project(val id: String, val isArchived: Boolean)
class FinishedBuild(val finishDate: Date, val build: Build)
class Build(val id: Long, val startDate: Date, val isFinished: Boolean, val duration: Long)