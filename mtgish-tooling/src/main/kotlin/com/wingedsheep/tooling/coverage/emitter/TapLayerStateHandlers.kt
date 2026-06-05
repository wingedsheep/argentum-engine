package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.asArr
import com.wingedsheep.tooling.coverage.asInt
import com.wingedsheep.tooling.coverage.findAdjustPt
import com.wingedsheep.tooling.coverage.findInteger
import com.wingedsheep.tooling.coverage.jsonContains
import com.wingedsheep.tooling.coverage.strField
import com.wingedsheep.tooling.coverage.subtypes
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/** Tap/untap, continuous P/T & keyword grants (CreatePermanentLayerEffectUntil), and turn-state
 *  effects (skip untap / skip combat / extra lands). */
internal val tapLayerStateHandlers: Map<String, ActionHandler> = buildMap {
    fun reg(vararg keys: String, h: ActionHandler) = keys.forEach { put(it, h) }

    reg("TapPermanent", "UntapPermanent") { node, args, tvar ->
        val tgt = refTarget(args, tvar) ?: return@reg null
        used.addAll(listOf("Effects", "EffectTarget"))
        "Effects.${if (node.strField("_Action") == "TapPermanent") "Tap" else "Untap"}($tgt)"
    }
    reg("TapEachPermanent", "UntapEachPermanent") { node, args, _ ->
        val verb = if (node.strField("_Action") == "TapEachPermanent") "Tap" else "Untap"
        if (jsonContains(node, "_Permanents", "Ref_TargetPermanents")) {  // Tidal Surge: each chosen target
            used.add("Effects"); return@reg "Effects.${verb}EachTarget()"
        }
        used.addAll(listOf("ForEachInGroupEffect", "Effects", "EffectTarget"))  // mass: tap/untap a group
        val filter = groupFilterDsl(args) ?: return@reg null
        "ForEachInGroupEffect($filter, Effects.$verb(EffectTarget.Self))"
    }

    reg("CreatePermanentLayerEffectUntil", "CreateEachPermanentLayerEffectUntil") { node, _, tvar ->
        renderLayerEffect(node, node.strField("_Action")!!, tvar)
    }

    reg("CreatePlayerEffectUntil") { node, _, _ ->  // Summer Bloom: may play N additional lands
        val n = findInteger(node)
        if (jsonContains(node, "_PlayerEffect", "MayPlayAdditionalLands") && n is Int) {
            used.add("PlayAdditionalLandsEffect"); "PlayAdditionalLandsEffect($n)"
        } else null
    }

    reg("EachPermanentDoesntUntapDuringControllersNextUntap") { _, _, tvar ->
        used.add("SkipUntapEffect"); if (tvar != null) "SkipUntapEffect($tvar)" else "SkipUntapEffect()"
    }
    reg("SkipAllCombatPhasesTheirNextTurn") { _, _, tvar ->
        used.add("SkipCombatPhasesEffect"); if (tvar != null) "SkipCombatPhasesEffect($tvar)" else "SkipCombatPhasesEffect()"
    }
}

/** CreatePermanentLayerEffectUntil / its each-permanent form -> ModifyStats / GrantKeyword,
 *  optionally over a group (ForEachInGroup). */
internal fun EmitCtx.renderLayerEffect(node: JsonObject, action: String, tvar: String?): String? {
    val mass = action == "CreateEachPermanentLayerEffectUntil"
    val target = if (mass) "EffectTarget.Self" else refTarget(node["args"], tvar)
    if (target == null) return null
    used.add("EffectTarget")
    val inner = mutableListOf<String>()
    val pt = findAdjustPt(node)
    if (pt is JsonArray && pt.size == 2) {
        used.add("ModifyStatsEffect")
        inner.add("ModifyStatsEffect(powerModifier = ${pt[0].asInt()}, toughnessModifier = ${pt[1].asInt()}, target = $target)")
    }
    if (jsonContains(node, "_LayerEffect", "AddAbility")) {
        var kw: String? = null
        if (jsonContains(node, "_Rule", "Landwalk")) {  // AddAbility{Landwalk{Forest}} -> FORESTWALK
            val subs = subtypes(node)
            if (subs.isNotEmpty() && (subs[0].uppercase() + "WALK") in keywords) kw = subs[0].uppercase() + "WALK"
        }
        kw = kw ?: keywordOf(node)
        if (kw != null) {
            used.addAll(listOf("GrantKeywordEffect", "Keyword"))
            inner.add("GrantKeywordEffect(Keyword.$kw, $target)")
        } else return null
    }
    if (inner.isEmpty()) return null
    var effect: String? = if (inner.size == 1) inner[0] else null
    if (effect == null) {
        used.add("CompositeEffect")
        effect = "CompositeEffect(listOf(" + inner.joinToString(", ") + "))"
    }
    if (mass) {
        used.add("ForEachInGroupEffect")
        val gfArg = (node["args"].asArr)?.getOrNull(0) ?: JsonObject(emptyMap())
        val filter = groupFilterDsl(gfArg) ?: return null
        return "ForEachInGroupEffect($filter, $effect)"
    }
    return effect
}
