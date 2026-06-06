package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.asArr
import com.wingedsheep.tooling.coverage.asInt
import com.wingedsheep.tooling.coverage.asObj
import com.wingedsheep.tooling.coverage.asStr
import com.wingedsheep.tooling.coverage.field
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
        // Printed oracle text is display/training data (the gym observation surfaces it to agents); the
        // engine still derives behaviour from the structured keywords/effects below, never from this string.
        scryfall?.strField("oracle_text")?.takeIf { it.isNotEmpty() }?.let { body.add("    oracleText = \"${ktStr(it)}\"") }
        if (pt != null) { body.add("    power = ${pt["Power"].asInt()}"); body.add("    toughness = ${pt["Toughness"].asInt()}") }
        // Emit keywords in printed (rule) order, not alphabetical — `keywordLines` collects them in the
        // card's rule order, which matches the hand-authored golden's `keywords(...)` order (e.g.
        // "Trample, haste" stays TRAMPLE, HASTE rather than re-sorting to HASTE, TRAMPLE).
        if (kw.isNotEmpty()) body.add("    keywords(${kw.joinToString(", ") { "Keyword.$it" }})")
        val cardLevelLines = ctx.cardLevelCastEffectLines(card) ?: return incomplete(ctx, body, scryfall, pkg)
        body.addAll(cardLevelLines)

        // Auras: the pure static-buff shape (EnchantPermanent + PermanentLayerEffect) renders faithfully,
        // but an activated/triggered ability on an Aura references its "enchanted creature" — context the
        // generic activated/trigger emitters don't model (they'd render a plain self/own-permanent ability).
        // e.g. the Onslaught Crowns' "Sacrifice this Aura: enchanted creature AND others sharing a type get …".
        // Scaffold those rather than emit a confidently-wrong complete render.
        if (jsonContains(card["Rules"], "_Rule", "EnchantPermanent")) {
            val unfaithfulOnAuras = setOf("Activated", "ActivatedWithModifiers", "TriggerA", "AsPermanentEnters", "FromAnyZone")
            (card["Rules"].asArr ?: JsonArray(emptyList())).forEach { r ->
                (r as? JsonObject)?.strField("_Rule")?.takeIf { it in unfaithfulOnAuras }?.let {
                    ctx.reasons.add("aura-with-$it"); return incomplete(ctx, body, scryfall, pkg)
                }
            }
        }

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
                rname == "TriggerOnceEachTurn" -> block = ctx.triggerBlock(rule, oncePerTurn = true)
                rname == "PermanentRuleEffect" -> block = ctx.staticBlock(rule)
                rname == "PlayerEffect" -> block = ctx.playerEffectBlock(rule)
                rname == "EnchantPermanent" -> block = ctx.auraTargetBlock(rule)
                rname == "PermanentLayerEffect" -> block = ctx.staticHostBlock(rule)
                rname == "AsPermanentEnters" -> block = ctx.asEntersBlock(rule)
                rname == "EachPermanentLayerEffect" -> block = ctx.staticLordBlock(rule)
                rname == "FromAnyZone" -> block = ctx.fromAnyZoneBlock(rule)
                rname == "CDA_Power" -> block = ctx.cdaStatsBlock(card, rule)
                rname == "CDA_Toughness" ->
                    if (jsonContains(card["Rules"], "_Rule", "CDA_Power")) continue  // emitted with CDA_Power
                    else { ctx.reasons.add("CDA_Toughness"); return incomplete(ctx, body, scryfall, pkg) }
                rname == "Activated" || rname == "ActivatedWithModifiers" -> block = ctx.activatedBlock(rule)
                rname == "Cycling" -> block = manaKeywordCost(rule)?.let { listOf("    keywordAbility(KeywordAbility.cycling(\"$it\"))") }
                rname == "Morph" -> block = manaKeywordCost(rule)?.let { listOf("    morph = \"$it\"") }
                rname == "Flashback" -> block = manaKeywordCost(rule)?.let { listOf("    keywordAbility(KeywordAbility.flashback(\"$it\"))") }
                rname == "Crew" -> block = rule["args"].asInt()?.let { listOf("    keywordAbility(KeywordAbility.crew($it))") }
                rname == "Protection" -> block = protectionScopeDsl(rule)?.let { listOf("    keywordAbility(KeywordAbility.Protection($it))") }
                rname != null && (rname in handledRules || Bridge[rname]?.kind == "keyword") -> continue
                // A bare auto-keyword rule (Flying, Menace, …) carries no args. A keyword rule that DOES
                // carry args is parameterized (Crew N, Flashback {cost}, Bushido N, …) — those must be
                // rendered exactly by an explicit case above or scaffold; never silently stamped bare,
                // which would drop the parameter.
                rname != null && pascalToUpperSnake(rname) in keywords && rule["args"] == null -> continue
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

    /** A keyword-ability whose cost is pure mana (`Cycling {2}`, `Morph {4}{W}`). Returns the rendered
     *  mana string, or null when the cost isn't simple mana — which downgrades the card to a scaffold
     *  rather than emit an inexact cost. */
    private fun manaKeywordCost(rule: JsonObject): String? {
        val cost = rule.field("args")
        if (cost.strField("_Cost") != "PayMana") return null
        return renderMana(cost.field("args"))
    }

    /** `Protection from X` -> the `ProtectionScope.*` DSL, or null (scaffold) for scopes the emitter
     *  can't render exactly yet. ONS only needs single/multi colour and creature-subtype. */
    private fun protectionScopeDsl(rule: JsonObject): String? {
        val inner = rule.field("args")
        return when (inner.strField("_Protectable")) {
            "FromColor" -> {
                val colorsNode = inner.field("args")
                if (colorsNode.strField("_ProtectableColor") != "Colors") return null
                val colors = (colorsNode.field("args").asArr ?: return null).mapNotNull { it.strField("_Color")?.uppercase() }
                when {
                    colors.isEmpty() -> null
                    colors.size == 1 -> "ProtectionScope.Color(Color.${colors[0]})"
                    else -> "ProtectionScope.Colors(setOf(${colors.joinToString(", ") { "Color.$it" }}))"
                }
            }
            "FromTypes" -> {
                val typesNode = inner.field("args")
                if (typesNode.strField("_Cards") != "IsCreatureType") return null
                typesNode.field("args").asStr()?.let { "ProtectionScope.Subtype(\"$it\")" }
            }
            else -> null
        }
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
