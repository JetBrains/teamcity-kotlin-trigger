package com.jetbrains.teamcity.boris.simplePlugin;

import jetbrains.buildServer.buildTriggers.*;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SimpleTrigger extends BuildTriggerService {
    public static String ENABLE_PROPERTY = "enable";
    public static String DELAY_PROPERTY = "delay";

    private final PluginDescriptor myPluginDescriptor;

    private Date previousCallDate = null;

    public SimpleTrigger(@NotNull final PluginDescriptor descriptor) {
        myPluginDescriptor = descriptor;
    }

    @NotNull
    @Override
    public String getName() {
        return "simpleTrigger";
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "A simple scheduling trigger";
    }

    @NotNull
    @Override
    public String describeTrigger(@NotNull BuildTriggerDescriptor buildTriggerDescriptor) {
        Map<String, String> properties = buildTriggerDescriptor.getProperties();
        if (!getEnable(properties)) return "Does nothing (edit configurations to activate it)";

        Integer delay = getDelay(properties);
        if (delay == null || delay <= 0) return "Incorrect state: wrong delay";

        StringBuilder period = new StringBuilder("minute");

        if (delay % 10 != 1)
            period.append("s");
        if (delay != 1)
            period.insert(0, " ").insert(0, delay);

        return "Initiates your build each " + period;
    }

    @Nullable
    @Override
    public PropertiesProcessor getTriggerPropertiesProcessor() {
        return properties -> {
            Collection<InvalidProperty> rv = new ArrayList<>();

            boolean enable = getEnable(properties);
            Integer delay = getDelay(properties);
            if (enable && (delay == null || delay <= 0)) {
                rv.add(new InvalidProperty(DELAY_PROPERTY, "Specify a correct delay, please"));
            }

            return rv;
        };
    }

    @Nullable
    @Override
    public Map<String, String> getDefaultTriggerProperties() {
        return new HashMap<String, String>() {{
            put(ENABLE_PROPERTY, Boolean.toString(true));
            put(DELAY_PROPERTY, "10");
        }};
    }

    @Override
    public String getEditParametersUrl() {
        return myPluginDescriptor.getPluginResourcesPath("simpleTrigger.jsp");
    }

    @Override
    public boolean isMultipleTriggersPerBuildTypeAllowed() {
        return true;
    }

    @NotNull
    @Override
    public BuildTriggeringPolicy getBuildTriggeringPolicy() {
        return new PolledBuildTrigger() {
            @Override
            public void triggerBuild(@NotNull PolledTriggerContext polledTriggerContext) throws BuildTriggerException {
                Map<String, String> properties = polledTriggerContext.getTriggerDescriptor().getProperties();
                Integer delay = getDelay(properties);

                if (!getEnable(properties) || null == delay) return;

                Date currentDate = new Date();
                if (previousCallDate == null || currentDate.getTime() - previousCallDate.getTime() >= delay * 60_000) {
                    polledTriggerContext.getBuildType().addToQueue(getName() + " " + currentDate);
                    previousCallDate = currentDate;
                }
            }
        };
    }

    private boolean getEnable(Map<String, String> properties) {
        return StringUtil.isTrue(properties.get(ENABLE_PROPERTY));
    }

    private Integer getDelay(Map<String, String> properties) {
        try {
            return Integer.parseInt(properties.get(DELAY_PROPERTY));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}