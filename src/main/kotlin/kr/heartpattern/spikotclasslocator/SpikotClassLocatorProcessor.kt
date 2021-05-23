package kr.heartpattern.spikotclasslocator

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.util.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import javax.tools.StandardLocation
import kotlin.collections.HashMap

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class SpikotClassLocatorProcessor : AbstractProcessor() {
    private val map = HashMap<String, LinkedList<String>>()
    private fun getTypeElement(name: String): TypeElement {
        return processingEnv.elementUtils.getTypeElement(name)
    }

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        // Find Annotation
        val findAnnotationElement = getTypeElement("kr.heartpattern.spikot.plugin.FindAnnotation")
        for (annotation in annotations) {
            val annotationMirrors = annotation.annotationMirrors
            val find = annotationMirrors.find { processingEnv.typeUtils.isSameType(it.annotationType, findAnnotationElement.asType()) }
                ?: continue
            val implements = find.elementValues.entries.find { it.key.simpleName.toString() == "impl" }?.value?.value as List<*>?
            val implementsTypeMirror = implements?.map {
                processingEnv.typeUtils.erasure(
                    getTypeElement((it as AnnotationValue).value.toString()).asType()
                )
            }
            val list = map.getOrPut(annotation.qualifiedName.toString()) { LinkedList() }
            elem@ for (element in roundEnv.getElementsAnnotatedWith(annotation)) {
                if (element.kind != ElementKind.CLASS || element !is TypeElement) {
                    processingEnv.messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "@${annotation.qualifiedName} can only annotate class",
                        element
                    )
                    return false
                }

                val erasured = processingEnv.typeUtils.erasure(element.asType())
                if (implementsTypeMirror != null) {
                    for (implement in implementsTypeMirror) {
                        if (!processingEnv.typeUtils.isSubtype(
                                erasured,
                                implement
                            )
                        ) {
                            processingEnv.messager.printMessage(
                                Diagnostic.Kind.ERROR,
                                "@${annotation.qualifiedName} can only annotate class which implement ${implements.joinToString { it.toString() }}",
                                element
                            )
                            return false
                        }
                    }
                }
                list.add(processingEnv.elementUtils.getBinaryName(element).toString())
            }
        }

        if (!roundEnv.processingOver()) return true
        processingEnv.filer.createResource(StandardLocation.CLASS_OUTPUT, "", "spikot-${UUID.randomUUID()}.json").openWriter().buffered().use { writer ->
            val json = JsonObject()
            for ((key, value) in map) {
                val array = JsonArray()
                for (clazz in value) {
                    array.add(clazz)
                }
                json.add(key, array)
            }
            writer.write(json.toString())
        }
        return true
    }
}
