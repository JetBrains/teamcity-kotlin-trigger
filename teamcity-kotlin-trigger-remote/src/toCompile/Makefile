KC = ${KOTLIN_HOME}\kotlinc
DOMAIN = jetbrains\buildServer\buildTriggers\remote
OPENAPI_PATH = ..\..\..\teamcity-kotlin-trigger-openapi\src\main\kotlin\$(DOMAIN)
IMPORTS = $(OPENAPI_PATH)\CustomTriggerPolicy.kt $(OPENAPI_PATH)\TriggerContext.kt $(OPENAPI_PATH)\BuildType.kt $(OPENAPI_PATH)\annotation\CustomTriggerProperties.kt
OUT = ..\compiled

.PHONY: clearUnnecessary

all: ScheduleTriggerPolicy.jar DemandTriggerPolicy.jar ErroneousTriggerPolicy.jar AlternatingTriggerPolicy.jar HangingTriggerPolicy.jar OutOfMemoryTriggerPolicy.jar

%.jar: %.kt $(IMPORTS)
	$(KC) $^ -d $(OUT)\$@
