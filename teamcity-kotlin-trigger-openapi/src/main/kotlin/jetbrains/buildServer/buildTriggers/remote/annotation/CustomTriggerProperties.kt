

package jetbrains.buildServer.buildTriggers.remote.annotation

@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class CustomTriggerProperties(vararg val properties: CustomTriggerProperty)

@Target(AnnotationTarget.CLASS)
@Repeatable
@MustBeDocumented
annotation class CustomTriggerProperty(
    val name: String,
    val type: PropertyType,
    val description: String,
    val required: Boolean = false
)

enum class PropertyType(val typeName: String) {
    TEXT("text"), BOOLEAN("boolean")
}