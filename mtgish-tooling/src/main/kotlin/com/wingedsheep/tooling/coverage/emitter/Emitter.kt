package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.asArr
import com.wingedsheep.tooling.coverage.asInt
import com.wingedsheep.tooling.coverage.asObj
import com.wingedsheep.tooling.coverage.asStr
import com.wingedsheep.tooling.coverage.asciiIdentifier
import com.wingedsheep.tooling.coverage.jsonContains
import com.wingedsheep.tooling.coverage.pascalToUpperSnake
import com.wingedsheep.tooling.coverage.strField
import com.wingedsheep.tooling.coverage.bridge.Bridge
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * mtgish -> Argentum cardDef EMITTER. The single source of truth for "what the
 * generator would emit": fidelity tiers cards on whether [renderCard] completes, autogen writes the
 * files, and the Kotlin gate compiles the output. A whole-card render is only a draft candidate;
 * calibrated sets must pass the gameplay-tree fidelity gate before the output is trusted.
 *
 * This facade just assembles a whole card; the per-action "mapping" rules live in the sibling files
 * (see [EmitCtx]). Imports resolve from a live SDK source scan so they can't rot.
 */
object Emitter {
    fun renderCard(
        card: JsonObject, scryfall: JsonObject?, effects: Set<String>, keywords: Set<String>,
        pkg: String = "com.wingedsheep.mtg.sets.generated.demo.cards",
    ): RenderResult {
        val ctx = EmitCtx(keywords, scryfall?.strField("oracle_text"))
        val name = card["Name"].asStr() ?: ""
        val ident = asciiIdentifier(name)
        val pt = card["CardPT"].asObj

        val permanent = isPermanent(card)
        val kw = if (permanent) keywordLines(card, keywords) else emptySet()

        val body = mutableListOf<String>()
        body.addAll(docCommentLines(card, scryfall))
        body.add("val $ident = card(\"${ktStr(name)}\") {")
        body.add("    manaCost = \"${renderMana(card["ManaCost"])}\"")
        colorIdentityDsl(scryfall)?.let { body.add("    colorIdentity = \"$it\"") }
        body.add("    typeLine = \"${renderTypeline(card["Typeline"])}\"")
        if (pt != null) { body.add("    power = ${pt["Power"].asInt()}"); body.add("    toughness = ${pt["Toughness"].asInt()}") }
        if (kw.isNotEmpty()) body.add("    keywords(${kw.sorted().joinToString(", ") { "Keyword.$it" }})")
        val cardLevelLines = ctx.cardLevelCastEffectLines(card) ?: return incomplete(ctx, body, scryfall, pkg)
        body.addAll(cardLevelLines)

        val handledRules = setOf("SpellActions", "TriggerA", "PermanentRuleEffect", "Flying", "Haste",
            "Vigilance", "Reach", "Defender", "Landwalk", "FirstStrike", "Trample", "CastEffect")
        for (rule in (card["Rules"].asArr ?: JsonArray(emptyList()))) {
            if (rule !is JsonObject) continue
            val rname = rule.strField("_Rule")
            val block: List<String>?
            when {
                rname == "CastEffect" -> {
                    if (!ctx.castEffectHandled(rule)) return incomplete(ctx, body, scryfall, pkg)
                    continue
                }
                rname == "SpellActions" -> block = ctx.spellBlock(card)
                rname == "TriggerA" -> block = ctx.triggerBlock(rule)
                rname == "PermanentRuleEffect" -> block = ctx.staticBlock(rule)
                rname == "Activated" || rname == "ActivatedWithModifiers" -> block = ctx.activatedBlock(rule)
                rname != null && (rname in handledRules || Bridge[rname]?.kind == "keyword") -> continue
                rname != null && pascalToUpperSnake(rname) in keywords -> continue  // auto-keyword rule
                else -> { ctx.reasons.add(rname ?: "unknown-rule"); return incomplete(ctx, body, scryfall, pkg) }
            }
            if (block == null) return incomplete(ctx, body, scryfall, pkg)
            body.addAll(block)
        }

        if (!permanent && !jsonContains(card["Rules"], "_Rule", "SpellActions")) {
            ctx.reasons.add("no-renderable-effect"); return incomplete(ctx, body, scryfall, pkg)
        }

        body.addAll(metadataLines(scryfall))
        body.add("}")
        return RenderResult(assemble(body, pkg, complete = true), true, ctx.reasons)
    }

    /** Landwalk-keyword recovery, exposed for the fidelity scorer's generated-capability set. */
    fun findLandwalkKeywords(node: JsonElement?, keywords: Set<String>, out: MutableSet<String>) =
        com.wingedsheep.tooling.coverage.emitter.findLandwalkKeywords(node, keywords, out)

    private fun incomplete(ctx: EmitCtx, body: List<String>, scryfall: JsonObject?, pkg: String): RenderResult {
        val b = body.toMutableList()
        b.add("    // STRUCTURE needs human wiring: ${ctx.reasons.sorted().joinToString(", ")}")
        b.addAll(metadataLines(scryfall))
        b.add("}")
        return RenderResult(assemble(b, pkg, complete = false), false, ctx.reasons)
    }
}
