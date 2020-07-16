KC = ${KOTLIN_HOME}\kotlinc
DOMAIN = jetbrains\buildServer\buildTriggers\remote
TRIGGER = ..\main\kotlin\$(DOMAIN)\Trigger.kt
CONSTANTS = ..\..\..\teamcity-kotlin-trigger-common\src\main\kotlin\$(DOMAIN)\Constants.kt
OUT = ..\main\resources

.PHONY: clearUnnecessary

package: ScheduleTrigger.jar DemandTrigger.jar ErroneousTrigger.jar

%.jar: %.kt $(TRIGGER) $(CONSTANTS)
	$(KC) $^ -d $(OUT)\$@

compile: doCompile clearUnnecessary

doCompile: ScheduleTrigger.kt DemandTrigger.kt ErroneousTrigger.kt $(TRIGGER) $(CONSTANTS)
	$(KC) $^ -d $(OUT)


clearUnnecessary:
	rm -rf $(OUT)\META-INF $(OUT)\$(DOMAIN)\Trigger.class $(OUT)\$(DOMAIN)\TriggerKt.class
	rm -f $(OUT)\$(DOMAIN)\Constants.class 
	rm -f $(OUT)\$(DOMAIN)\Constants\$$Request.class
	rm -f $(OUT)\$(DOMAIN)\Constants\$$Response.class
