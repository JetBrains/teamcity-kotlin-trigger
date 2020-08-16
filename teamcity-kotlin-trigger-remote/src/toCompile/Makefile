KC = ${KOTLIN_HOME}\kotlinc
DOMAIN = jetbrains\buildServer\buildTriggers\remote
IMPORTS = ..\main\kotlin\$(DOMAIN)\CustomTriggerPolicy.kt ..\..\..\teamcity-kotlin-trigger-common\src\main\kotlin\$(DOMAIN)\RequestBody.kt ..\..\..\teamcity-kotlin-trigger-common\src\main\kotlin\$(DOMAIN)\BuildType.kt
OUT = ..\compiled

.PHONY: clearUnnecessary

all: ScheduleTriggerPolicy.jar DemandTriggerPolicy.jar ErroneousTriggerPolicy.jar AlternatingTriggerPolicy.jar HangingTriggerPolicy.jar OutOfMemoryTriggerPolicy.jar

%.jar: %.kt $(IMPORTS)
	$(KC) $^ -d $(OUT)\$@
