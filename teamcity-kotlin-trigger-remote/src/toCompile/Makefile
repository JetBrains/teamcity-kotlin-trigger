KC = ${KOTLIN_HOME}\kotlinc
DOMAIN = jetbrains\buildServer\buildTriggers\remote
IMPORTS = ..\main\kotlin\$(DOMAIN)\Trigger.kt ..\..\..\teamcity-kotlin-trigger-common\src\main\kotlin\$(DOMAIN)\Request.kt
OUT = ${ROOT_PLUGIN_DATA}\teamcityKotlinTrigger

.PHONY: clearUnnecessary

all: ScheduleTrigger.jar DemandTrigger.jar ErroneousTrigger.jar

%.jar: %.kt $(IMPORTS)
	$(KC) $^ -d $(OUT)\$@
