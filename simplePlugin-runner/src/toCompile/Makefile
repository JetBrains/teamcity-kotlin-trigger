KC = ${KOTLIN_HOME}\kotlinc
DOMAIN = com\jetbrains\teamcity\boris\simplePlugin
TRIGGER = ..\main\kotlin\$(DOMAIN)\Trigger.kt
OUT = ..\main\resources

all: compile clearUnnecessary

compile: TriggerImpl.kt $(TRIGGER)
	$(KC) TriggerImpl.kt $(TRIGGER) -d $(OUT)


clearUnnecessary:
	rm -rf $(OUT)\META-INF $(OUT)\$(DOMAIN)\Trigger.class $(OUT)\$(DOMAIN)\TriggerKt.class
