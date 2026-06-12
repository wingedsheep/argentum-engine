package com.wingedsheep.tooling.coverage

/**
 * The capability registry scanned from Kotlin source so it can't rot: the set of effect SerialNames
 * and Keyword members the bridge validates against, plus the symbol→import resolver the emitter's
 * auto-import derivation uses. The mtgish→Argentum bridge itself lives as typed Kotlin under the
 * `bridge/` package — see [com.wingedsheep.tooling.coverage.bridge.Bridge].
 */
object Registry {
    /** Effect `@SerialName`s scanned from the effects package (nested-enum serialnames dropped). */
    fun loadEffectSerialNames(): Set<String> {
        val tags = mutableSetOf<String>()
        val re = Regex("""@SerialName\("([^"]+)"\)""")
        SDK_EFFECTS.listFiles { f -> f.name.endsWith(".kt") }?.forEach { kt ->
            re.findAll(kt.readText()).forEach { m ->
                val tag = m.groupValues[1]
                if ("." !in tag) tags.add(tag)  // drop nested-enum serialnames (SuccessCriterion.Auto)
            }
        }
        return tags
    }

    /** The `Keyword` enum members. */
    fun loadKeywords(): Set<String> =
        Regex("""^\s+([A-Z][A-Z0-9_]+)\b""", RegexOption.MULTILINE)
            .findAll(KEYWORD_KT.readText())
            .map { it.groupValues[1] }
            .toSet()

    // --- import resolution: symbol -> package, from a live scan of the SDK source (anti-rot) -----
    // For `fun`/`val`, the trailing guard rejects extension declarations: in
    // `fun GameObjectFilter.namedFromVariable(...)` the captured identifier is the *receiver
    // type*, not a declaration in this package — indexing it would shadow the type's real home
    // (the receiver's name is followed by `.`, or by `<` for a generic receiver, while a true
    // function/property name is followed by `(`, `=`, `:`, or whitespace).
    private val DECL = Regex(
        """^(?:public\s+|internal\s+)?(?:sealed\s+|abstract\s+|open\s+|data\s+|value\s+)?""" +
            """(?:(?:class|object|interface|enum class)\s+([A-Za-z_][A-Za-z0-9_]*)""" +
            """|(?:fun|val)\s+(?:<[^>]*>\s+)?([A-Za-z_][A-Za-z0-9_]*)(?![\w.<]))"""
    )
    private val PKG_PREF = listOf(
        "com.wingedsheep.sdk.dsl", "com.wingedsheep.sdk.scripting.effects",
        "com.wingedsheep.sdk.scripting.targets", "com.wingedsheep.sdk.scripting.filters.unified",
        "com.wingedsheep.sdk.scripting", "com.wingedsheep.sdk.scripting.values",
        "com.wingedsheep.sdk.core", "com.wingedsheep.sdk.model",
    )

    private val symbolIndex: Map<String, String> by lazy {
        val idx = mutableMapOf<String, MutableSet<String>>()
        SDK_ROOT.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { kt ->
            var pkg: String? = null
            for (line in kt.readText().lineSequence()) {
                val pm = Regex("""^package\s+([\w.]+)""").find(line)
                if (pm != null) { pkg = pm.groupValues[1]; continue }
                if (pkg != null && line.isNotEmpty() && !line[0].isWhitespace()) {
                    DECL.find(line)?.let { m ->
                        val name = m.groupValues[1].ifEmpty { m.groupValues[2] }
                        idx.getOrPut(name) { mutableSetOf() }.add(pkg!!)
                    }
                }
            }
        }
        idx.mapValues { (_, pkgs) ->
            pkgs.sortedWith(compareBy({ PKG_PREF.indexOf(it).let { i -> if (i < 0) 99 else i } }, { it }))[0]
        }
    }

    fun importFor(sym: String): String? = symbolIndex[sym]?.let { "$it.$sym" }

    // --- Subtype value -> named companion constant, scanned from Subtype.kt (anti-rot) -----------
    private val SUBTYPE_DECL = Regex("""\bval\s+([A-Z][A-Z0-9_]*)\s*=\s*Subtype\("([^"]+)"\)""")

    private val subtypeConstants: Map<String, String> by lazy {
        SUBTYPE_DECL.findAll(SUBTYPE_KT.readText()).associate { it.groupValues[2] to it.groupValues[1] }
    }

    /** The `Subtype.X` constant name for a subtype value (e.g. "Plains" -> "PLAINS"), or null. */
    fun subtypeConstant(value: String): String? = subtypeConstants[value]
}
