KC = ${KOTLIN_HOME}\kotlinc
DOMAIN = jetbrains\buildServer\buildTriggers\remote
IMPORTS = ..\main\kotlin\$(DOMAIN)\Trigger.kt ..\..\..\teamcity-kotlin-trigger-common\src\main\kotlin\$(DOMAIN)\Request.kt
OUT = ..\main\resources

.PHONY: clearUnnecessary

package: ScheduleTrigger.jar DemandTrigger.jar ErroneousTrigger.jar

%.jar: %.kt $(IMPORTS)
	$(KC) $^ -d $(OUT)\$@

compile: doCompile clearUnnecessary

doCompile: ScheduleTrigger.kt DemandTrigger.kt ErroneousTrigger.kt $(IMPORTS)
	$(KC) $^ -d $(OUT)


clearUnnecessary:
	rm -rf $(OUT)\META-INF $(OUT)\$(DOMAIN)\Trigger.class $(OUT)\$(DOMAIN)\TriggerKt.class
	rm -f $(OUT)\$(DOMAIN)\Constants.class 
	rm -f $(OUT)\$(DOMAIN)\Constants\$$Request.class
	rm -f $(OUT)\$(DOMAIN)\Constants\$$Response.class
