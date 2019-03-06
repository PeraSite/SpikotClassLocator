package io.github.ReadyMadeProgrammer.SpikotClassLocator

import com.github.salomonbrys.kotson.set
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.github.ReadyMadeProgrammer.Spikot.command.CommandHandler
import io.github.ReadyMadeProgrammer.Spikot.command.RootCommand
import io.github.ReadyMadeProgrammer.Spikot.config.Config
import io.github.ReadyMadeProgrammer.Spikot.config.ConfigSpec
import io.github.ReadyMadeProgrammer.Spikot.persistence.gson.Serializer
import io.github.ReadyMadeProgrammer.Spikot.module.IModule
import io.github.ReadyMadeProgrammer.Spikot.module.Module
import io.github.ReadyMadeProgrammer.Spikot.persistence.Data
import io.github.ReadyMadeProgrammer.Spikot.persistence.gson.GsonSerializer
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import javax.tools.StandardLocation

@SupportedAnnotationTypes("io.github.ReadyMadeProgrammer.Spikot.*")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class SpikotClassLocatorProcessor : AbstractProcessor() {
    private lateinit var messager: Messager
    private lateinit var pEnv: ProcessingEnvironment
    private val modules = HashSet<TypeElement>()
    private val commands = HashSet<TypeElement>()
    private val configs = HashSet<TypeElement>()
    private val serializers = HashSet<TypeElement>()
    private val datas = HashSet<TypeElement>()

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        messager = processingEnv.messager
        pEnv = processingEnv
    }

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        messager.printMessage(Diagnostic.Kind.NOTE, "Start spikot class locator")
        check<Module, IModule>(roundEnv, modules, "Only class or object implement IModule can annotated with @Module")
        check<RootCommand, CommandHandler>(roundEnv, commands, "Only class extends CommandHandler can annotated with @RootCommand")
        check<Config, ConfigSpec>(roundEnv, configs, "Only object extends ConfigSpec can annotated with @Config")
        check<Serializer, GsonSerializer<*>>(roundEnv, serializers, "Only class or object implement GsonSerializer can annotated with @Serializer")
        check<Data, Any>(roundEnv, datas, "Cannot register data class properly")
        if (!roundEnv.processingOver()) return true
        pEnv.filer.createResource(StandardLocation.CLASS_OUTPUT, "", "spikot.json").openWriter().buffered().run {
            val json = JsonObject()
            json["module"] = modules.toJsonArray()
            json["command"] = commands.toJsonArray()
            json["config"] = configs.toJsonArray()
            json["serializer"] = serializers.toJsonArray()
            json["data"] = datas.toJsonArray()
            val gson = GsonBuilder().setPrettyPrinting().create()
            write(gson.toJson(json))
            close()
        }
        messager.printMessage(Diagnostic.Kind.NOTE, "End spikot class locator")
        return true
    }

    private inline fun <reified T : Annotation, reified S : Any> check(roundEnv: RoundEnvironment, set: MutableSet<TypeElement>, error: String) {
        roundEnv.getElementsAnnotatedWith(T::class.java).forEach { element ->
            if (element.kind != ElementKind.CLASS || element !is TypeElement
                    || !pEnv.typeUtils.isSubtype(element.asType(), pEnv.elementUtils.getTypeElement(S::class.java.name).asType())) {
                messager.printMessage(Diagnostic.Kind.ERROR, error, element)
            } else {
                messager.printMessage(Diagnostic.Kind.NOTE, "Find ${T::class.simpleName}: ${element.qualifiedName}")
                set.add(element)
            }
        }
    }

    private fun HashSet<TypeElement>.toJsonArray(): JsonArray {
        val jsonArray = JsonArray()
        iterator().forEach {
            jsonArray.add(it.qualifiedName.toString())
        }
        return jsonArray
    }
}
