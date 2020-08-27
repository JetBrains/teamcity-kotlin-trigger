package jetbrains.buildServer.buildTriggers.remote

import java.util.*

class BuildType(
    val id: String,
    val isInQueue: Boolean,
    val isPaused: Boolean,
    val project: Project,
    val runningBuilds: List<RunningBuild>,
    val history: List<FinishedBuild>,
    val lastChangesStartedBuild: Build?,
    val lastChangesSuccessfullyFinished: Build?,
    val lastChangesFinished: Build?,
    val buildParameters: Map<String, String>,
    val parameters: Map<String, String>
)

class Project(val id: String, val isArchived: Boolean)

class Build(val id: Long, val startDate: Date, val isFinished: Boolean, val duration: Long)
class RunningBuild(val build: Build, val agentId: Int)
class FinishedBuild(val build: Build, val finishDate: Date)
