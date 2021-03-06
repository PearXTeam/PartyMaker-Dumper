package ru.pearx.pmdumper.utils

import net.minecraft.util.ResourceLocation
import org.apache.commons.lang3.reflect.FieldUtils
import ru.pearx.pmdumper.PMDumper
import java.lang.reflect.Field

fun Boolean.toPlusMinusString() = if (this) "+" else "-"

fun Int.toHexColorString() = "#${Integer.toHexString(this).toUpperCase().padStart(6, '0')}"

fun ResourceLocation.toPath(topPath: String = "", postfix: String = "") = "assets/$namespace/$topPath${if (topPath.isEmpty()) "" else "/"}$path$postfix"

fun ResourceLocation.toTexturesPath(pngPostfix: Boolean = true) = toPath("textures", if (pngPostfix) ".png" else "")

fun <T> mutableListOfNotNull(vararg elements: T?): MutableList<T> {
    val lst = ArrayList<T>(elements.size)
    for (element in elements)
        if (element != null)
            lst.add(element)
    return lst
}

private val fieldCache = hashMapOf<String, Field?>()

fun Any.readFieldRaw(vararg names: String): Any {
    for (name in names) {
        val field = run {
            val key = "${this::class.java.name}.$name"
            if (key in fieldCache) // can't use getOrPut here as of values can be null
                fieldCache[key]
            else {
                FieldUtils.getField(this::class.java, name, true).also { fieldCache[key] = it }
            }
        }
        if (field != null) {
            return field.get(this)
        }
    }
    throw NoSuchFieldException("No field with names ${names.contentToString()} found in ${this::class.java}.")
}

inline fun <reified T> Any.readField(vararg names: String): T = readFieldRaw(*names) as T

suspend inline fun SequenceScope<List<String>>.tryDump(list: MutableList<String>, block: MutableList<String>.() -> Unit) {
    try {
        list.block()
    }
    catch (e: Exception) {
        PMDumper.log.error("An error occurred while creating a dump! Already dumped data: ${list.joinToString(prefix = "[", separator = ", ", postfix = "]")}", e)
        list.add("*error*")
    }
    yield(list)
}

inline fun MutableList<String>.add(block: () -> String) {
    try {
        add(block())
    }
    catch(e: Exception) {
        add("*error*")
    }
}