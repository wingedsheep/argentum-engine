@file:OptIn(kotlin.js.ExperimentalJsExport::class)
package com.wingedsheep.assay.browser

import com.wingedsheep.assay.compile.CardCompiler
import com.wingedsheep.assay.compile.CompileResult
import com.wingedsheep.assay.gate.Touchstone
import kotlinx.serialization.json.*
import kotlin.js.JsExport

private val touchstone by lazy { Touchstone() }

/** Whole-card compatibility, with line round trips and header/SDK validation. No corpus access. */
@JsExport
fun checkOracleCard(input: String): String {
    if (input.length > 24000) return """{"status":"invalid_input","parse_complete":false,"error":"Input exceeds 24 KB."}"""
    return try {
        val result = CardCompiler.compile(input, touchstone)
        buildJsonObject {
            put("status", if (result is CompileResult.Compiled) "accepted" else "unsupported")
            put("parse_complete", result is CompileResult.Compiled)
            putJsonArray("faces") {
                result.assay?.faces?.forEach { face -> add(buildJsonObject {
                    put("normalization_holds", face.normalizationHolds)
                    put("restore_holds", face.restoreHolds?.let { JsonPrimitive(it) } ?: JsonNull)
                    putJsonArray("lines") { face.lines.forEach { line -> add(buildJsonObject {
                        put("index", line.index); put("input", line.line)
                        put("verdict", line.verdict.name)
                        put("printed", line.printed?.let { JsonPrimitive(it) } ?: JsonNull)
                        put("decline_token", line.declineToken?.let { JsonPrimitive(it) } ?: JsonNull)
                    }) } }
                }) }
            }
            when (result) {
                is CompileResult.Compiled -> putJsonArray("warnings") { result.warnings.forEach { add(JsonPrimitive(it)) } }
                is CompileResult.Declined -> putJsonArray("declines") { result.declines.forEach { decline -> add(buildJsonObject {
                    put("kind", decline.kind.name); put("detail", decline.detail)
                    put("line_index", decline.lineIndex?.let { JsonPrimitive(it) } ?: JsonNull)
                }) } }
            }
        }.toString()
    } catch (error: Exception) {
        buildJsonObject { put("status", "parser_error"); put("parse_complete", false); put("error", error.message ?: "Parser failed.") }.toString()
    }
}
