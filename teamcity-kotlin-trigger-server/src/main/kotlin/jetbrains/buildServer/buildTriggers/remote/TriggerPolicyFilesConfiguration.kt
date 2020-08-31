package jetbrains.buildServer.buildTriggers.remote

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class TriggerPolicyFilesConfiguration {
    @Bean
    open fun getTriggerPolicyFileManager(): TriggerPolicyFileManager<*> = SimpleTriggerPolicyJarManager()
}