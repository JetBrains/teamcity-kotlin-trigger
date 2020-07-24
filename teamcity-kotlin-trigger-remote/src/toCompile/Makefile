KC = ${KOTLIN_HOME}\kotlinc
DOMAIN = jetbrains\buildServer\buildTriggers\remote
IMPORTS = ..\main\kotlin\$(DOMAIN)\TriggerService.kt ..\..\..\teamcity-kotlin-trigger-common\src\main\kotlin\$(DOMAIN)\RequestBody.kt
OUT = ${ROOT_PLUGIN_DATA}\teamcityKotlinTrigger

.PHONY: clearUnnecessary

all: ScheduleTriggerService.jar DemandTriggerService.jar ErroneousTriggerService.jar AlternatingTriggerService.jar

%.jar: %.kt $(IMPORTS)
	$(KC) $^ -d $(OUT)\$@
