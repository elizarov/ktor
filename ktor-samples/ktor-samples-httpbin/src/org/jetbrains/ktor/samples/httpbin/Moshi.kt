package org.jetbrains.ktor.samples.httpbin

import com.squareup.moshi.*
import com.squareup.moshi.Moshi
import org.jetbrains.ktor.request.*
import org.jetbrains.ktor.util.*
import java.util.*
import kotlin.collections.Map
import kotlin.collections.component1
import kotlin.collections.component2

/** moshi Json Library ***/
object Moshi {
    val moshi = Moshi.Builder().add(MapAdapter()).build()

    val Map = moshi.adapter(Map::class.java).lenient().indent("  ")

    val JsonResponse = moshi.adapter(HttpBinResponse::class.java).indent("  ")

    /** See MapAdapter#serializeHttpbinError **/
    val Errors = moshi.adapter(HttpBinError::class.java).indent("  ")

    val ValuesMap = moshi.adapter(ValuesMap::class.java).indent("  ")

    fun parseJsonAsMap(json: String): Map<String, Any>? = try {
        @Suppress("UNCHECKED_CAST")
        Map.fromJson(json) as Map<String, Any>
    } catch (e: Exception) {
        println(e.message)
        null
    }

    private class MapAdapter {

        @ToJson fun serializeHttpBinError(error: HttpBinError): SerializedError {
            val stacktrace =
                    if (error.cause == null) null
                    else error.cause.stackTrace.take(3).map { e -> e.toString() }
            val result = SerializedError(
                    message = error.message,
                    method = error.request.httpMethod.value,
                    url = error.request.url(),
                    stacktrace = stacktrace
            )
            return result
        }

        @ToJson fun serializeFilePart(part: PartData.FileItem): String {
            return "File ${part.originalFileName} of type ${part.contentType}"
        }


        @ToJson fun serializeValuesMap(parseMap: ValuesMap): Map<Any, Any> {
            val result = LinkedHashMap<Any, Any>()
            for ((key, value) in parseMap.entries()) {
                if (value.size == 1) {
                    result.put(key, value[0])
                } else {
                    result.put(key, value)
                }
            }
            return result
        }

        @ToJson fun MultiPartData(multiPartData: MultiPartData?): Map<Any, Any>? {
            if (multiPartData == null) {
                return null
            }
            val result = LinkedHashMap<Any, Any>()
            for (part in multiPartData.parts) {
                when (part) {
                    is PartData.FormItem -> result.put(part.partName!!, part.value)
                    is PartData.FileItem -> result.put(part.partName!!, "A file of type ${part.contentType}")
                }
            }
            return result
        }

        @FromJson fun unserializeMultiPartDataserialize(map: Map<String, String>): MultiPartData?
                = TODO("Required by moshi but not needed")

        @FromJson fun unserializeValuesMap(map: Map<String, String>): ValuesMap
                = TODO("Required by moshi but not needed")

        @FromJson fun unserializeHttpbinError(map: SerializedError): HttpBinError
                = TODO("Required by moshi but not needed")

        @FromJson fun unserializeFilePart(json: String): PartData.FileItem
                = TODO("Required by moshi but not needed")
    }
}

private data class SerializedError(val message: String, val method: String, val url: String, val stacktrace: List<String>?)
