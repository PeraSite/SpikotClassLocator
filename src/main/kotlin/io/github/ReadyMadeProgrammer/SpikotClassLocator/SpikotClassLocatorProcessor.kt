package io.github.ReadyMadeProgrammer.SpikotClassLocator

import io.github.ReadyMadeProgrammer.Spikot.IModule
import io.github.ReadyMadeProgrammer.Spikot.Module
import io.github.ReadyMadeProgrammer.Spikot.command.CommandHandler
import io.github.ReadyMadeProgrammer.Spikot.command.RootCommand
import io.github.ReadyMadeProgrammer.Spikot.gson.GsonSerializer
import io.github.ReadyMadeProgrammer.Spikot.gson.Serializer
import io.github.ReadyMadeProgrammer.Spikot.i18n.Message
import io.github.ReadyMadeProgrammer.Spikot.i18n.MessageKey
import io.github.ReadyMadeProgrammer.Spikot.persistence.Data
import io.github.ReadyMadeProgrammer.Spikot.persistence.PlayerData
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
    private val modules = mutableSetOf<String>()
    private val commands = mutableSetOf<String>()
    private val messages = mutableSetOf<String>()
    private val serializers = mutableSetOf<String>()
    private val playerDatas = mutableSetOf<String>()
    private val pluginDatas = mutableSetOf<String>()
    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        messager = processingEnv.messager
        pEnv = processingEnv
    }

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        messager.printMessage(Diagnostic.Kind.NOTE, "Processing Spikot Class Locator")

        roundEnv.getElementsAnnotatedWith(Module::class.java).forEach { element ->
            if (element.kind != ElementKind.CLASS || !pEnv.typeUtils.isSubtype(element.asType(), pEnv.elementUtils.getTypeElement(IModule::class.java.name).asType()) || element !is TypeElement) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Only class implement IModule can annotated with @Module", element)
                return true
            }
            modules.add(element.qualifiedName.toString())
        }
        roundEnv.getElementsAnnotatedWith(RootCommand::class.java).forEach { element ->
            if (element.kind != ElementKind.CLASS || !pEnv.typeUtils.isSubtype(element.asType(), pEnv.elementUtils.getTypeElement(CommandHandler::class.java.name).asType()) || element !is TypeElement) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Only class implement CommandHandler can annotated with @RootCommand", element)
                return true
            }
            commands.add(element.qualifiedName.toString())
        }
        roundEnv.getElementsAnnotatedWith(Message::class.java).forEach { element ->
            if (element.kind != ElementKind.ENUM || !pEnv.typeUtils.isSubtype(element.asType(), pEnv.elementUtils.getTypeElement(MessageKey::class.java.name).asType()) || element !is TypeElement) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Only enum implement MessageKey can annotated with @Message", element)
                return true
            }
            messages.add(element.qualifiedName.toString())
        }
        roundEnv.getElementsAnnotatedWith(Serializer::class.java).forEach { element ->
            if(element.kind != ElementKind.CLASS || !pEnv.typeUtils.isSubtype(pEnv.typeUtils.erasure(element.asType()), pEnv.typeUtils.erasure(pEnv.elementUtils.getTypeElement(GsonSerializer::class.java.name).asType())) || element !is TypeElement){
                messager.printMessage(Diagnostic.Kind.ERROR, "Only class implement GsonSerializer can annotated with @SerializerAnnotation", element)
                return true
            }
            serializers.add(element.qualifiedName.toString())
        }
        roundEnv.getElementsAnnotatedWith(PlayerData::class.java).forEach{ element->
            if(element.kind != ElementKind.CLASS || element !is TypeElement){
                messager.printMessage(Diagnostic.Kind.ERROR,"Only class can annotated with @PlayerData", element)
                return true
            }
            playerDatas.add(element.qualifiedName.toString())
        }
        roundEnv.getElementsAnnotatedWith(Data::class.java).forEach{ element ->
            if(element.kind != ElementKind.CLASS || element !is TypeElement){
                messager.printMessage(Diagnostic.Kind.ERROR, "Only class can annotated with @Data", element)
                return true
            }
            pluginDatas.add(element.qualifiedName.toString())
        }
        if (!roundEnv.processingOver()) return true
        pEnv.filer.createResource(StandardLocation.CLASS_OUTPUT, "", "modules").openWriter().buffered().run {
            modules.forEach {
                appendln(it)
            }
            close()
        }
        pEnv.filer.createResource(StandardLocation.CLASS_OUTPUT, "", "commands").openWriter().buffered().run {
            commands.forEach {
                appendln(it)
            }
            close()
        }
        pEnv.filer.createResource(StandardLocation.CLASS_OUTPUT, "", "messages").openWriter().buffered().run {
            messages.forEach {
                appendln(it)
            }
            close()
        }
        pEnv.filer.createResource(StandardLocation.CLASS_OUTPUT,"","serializers").openWriter().buffered().run{
            serializers.forEach{
                appendln(it)
            }
            close()
        }
        pEnv.filer.createResource(StandardLocation.CLASS_OUTPUT,"","playerdatas").openWriter().buffered().run{
            playerDatas.forEach{
                appendln(it)
            }
            close()
        }
        pEnv.filer.createResource(StandardLocation.CLASS_OUTPUT, "", "plugindatas").openWriter().buffered().run{
            pluginDatas.forEach{
                appendln(it)
            }
            close()
        }
        return true
    }
}
