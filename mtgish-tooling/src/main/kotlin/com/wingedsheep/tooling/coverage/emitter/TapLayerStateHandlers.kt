package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.asArr
import com.wingedsheep.tooling.coverage.asInt
import com.wingedsheep.tooling.coverage.compact
import com.wingedsheep.tooling.coverage.findInteger
import com.wingedsheep.tooling.coverage.jsonContains
import com.wingedsheep.tooling.coverage.pascalToUpperSnake
import com.wingedsheep.tooling.coverage.strField
import com.wingedsheep.tooling.coverage.subtypes
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** Tap/untap, continuous P/T & keyword grants (CreatePermanentLayerEffectUntil), and turn-state
 *  effects (skip untap / skip combat / extra lands). */
internal val tapLayerStateHandlers: Map<String, ActionHandler> = actionHandlers {

    on("TapPermanent", "UntapPermanent") { node, args, tvar ->
        val tgt = refTarget(args, tvar) ?: return@on null
        "Effects.${if (node.strField("_Action") == "TapPermanent") "Tap" else "Untap"}($tgt)"
    }
    on("GoadCreature") { _, args, tvar ->  // CR 701.15: goad target creature
        val tgt = refTarget(args, tvar) ?: return@on null
        "Effects.Goad($tgt)"
    }
    on("RemoveCreatureFromCombat") { _, args, tvar ->  // "remove it from combat" (Gustcloak cycle)
        val tgt = refTarget(args, tvar) ?: return@on null
        "Effects.RemoveFromCombat($tgt)"
    }
    on("RegeneratePermanent") { _, args, _ ->
        // Self-regeneration ("{cost}: Regenerate this") renders faithfully. A chosen target's
        // requirement isn't always recovered exactly (e.g. "Regenerate target Zombie" flattens the
        // subtype to "permanent"), so scaffold the targeted case rather than emit a too-broad target.
        if (!jsonContains(args, "_Permanent", "ThisPermanent")) return@on null
        "RegenerateEffect(EffectTarget.Self)"
    }
    on("TapEachPermanent", "UntapEachPermanent") { node, args, _ ->
        val verb = if (node.strField("_Action") == "TapEachPermanent") "Tap" else "Untap"
        if (jsonContains(node, "_Permanents", "Ref_TargetPermanents")) {  // Tidal Surge: each chosen target
            return@on "Effects.${verb}EachTarget()"
        }
        val filter = groupFilterDsl(args) ?: return@on null  // mass: tap/untap a group
        "Effects.ForEachInGroup($filter, Effects.$verb(EffectTarget.Self))"
    }

    on("PutACounterOfTypeOnPermanent") { _, args, tvar ->
        // "Put a +1/+1 (or -1/-1) counter on <permanent>." Only the bare ±1/±1 PTCounter renders; any
        // other counter kind scaffolds rather than guess. The subject ref is self or the bound target.
        val arr = args.asArr ?: return@on null
        val counterNode = arr.getOrNull(0) as? JsonObject ?: return@on null
        if (counterNode.strField("_CounterType") != "PTCounter") return@on null
        val pt = counterNode["args"].asArr ?: return@on null
        val counter = when (Pair(pt.getOrNull(0).asInt(), pt.getOrNull(1).asInt())) {
            Pair(1, 1) -> "Counters.PLUS_ONE_PLUS_ONE"
            Pair(-1, -1) -> "Counters.MINUS_ONE_MINUS_ONE"
            else -> return@on null
        }
        val tgt = refTarget(arr.getOrNull(1), tvar) ?: return@on null
        "AddCountersEffect(counterType = $counter, count = 1, target = $tgt)"
    }

    on("CreatePermanentLayerEffectUntil", "CreateEachPermanentLayerEffectUntil") { node, _, tvar ->
        // "Enchanted creature and other creatures that share a creature type with it get …" (Onslaught
        // Crowns) — a group keyed to the host permanent, which the generic ForEachInGroup can't express.
        enchantedTypeGroupGrant(node)?.let { return@on it }
        renderLayerEffect(node, node.strField("_Action")!!, tvar)
    }

    on("CreatePlayerEffectUntil") { node, _, _ ->  // Summer Bloom: may play N additional lands
        val n = findInteger(node)
        if (jsonContains(node, "_PlayerEffect", "MayPlayAdditionalLands") && n is Int) {
            "PlayAdditionalLandsEffect($n)"
        } else null
    }

    on("EachPermanentDoesntUntapDuringControllersNextUntap") { _, _, tvar ->
        if (tvar != null) "SkipUntapEffect($tvar)" else "SkipUntapEffect()"
    }
    on("SkipAllCombatPhasesTheirNextTurn") { _, _, tvar ->
        if (tvar != null) "SkipCombatPhasesEffect($tvar)" else "SkipCombatPhasesEffect()"
    }
}

/** CreatePermanentLayerEffectUntil / its each-permanent form -> ModifyStats / GrantKeyword,
 *  optionally over a group (ForEachInGroup). The `args` are always `[target/filter, [layerEffects],
 *  expiration]`; EVERY entry in the layer-effects list must render, or the whole card scaffolds — a
 *  layer effect we silently drop (e.g. an AddAbility granting a triggered ability alongside a P/T
 *  buff) would emit a confidently-wrong card. The `_Expiration` is honoured exactly: end-of-turn uses
 *  the default-duration facade; "for as long as it remains tapped" carries an explicit
 *  Duration.WhileSourceTapped(); any other expiration scaffolds rather than emit a wrong duration. */
internal fun EmitCtx.renderLayerEffect(node: JsonObject, action: String, tvar: String?): String? {
    val mass = action == "CreateEachPermanentLayerEffectUntil"
    val target = if (mass) "EffectTarget.Self" else refTarget(node["args"], tvar)
    if (target == null) return null
    val duration = expirationDsl(node) ?: return null  // unknown expiration -> SCAFFOLD
    val durArg = if (duration.isEmpty()) "" else ", $duration"

    // The layer-effects list (mtgish always shapes the action's args as [target/filter, list, expiration]).
    val layerEffects = (node["args"].asArr)?.getOrNull(1) as? JsonArray ?: return null
    if (layerEffects.isEmpty()) return null
    val inner = mutableListOf<String>()
    for (le in layerEffects) {
        val leObj = le as? JsonObject ?: return null
        when (leObj.strField("_LayerEffect")) {
            "AdjustPT" -> {
                val pt = leObj["args"].asArr
                if (pt == null || pt.size != 2) return null
                // ModifyStats' facade carries no duration param, so a non-default duration uses the raw effect.
                inner.add(
                    if (duration.isEmpty()) "Effects.ModifyStats(${pt[0].asInt()}, ${pt[1].asInt()}, $target)"
                    else "ModifyStatsEffect(${pt[0].asInt()}, ${pt[1].asInt()}, $target, $duration)"
                )
            }
            "AddAbility" -> {
                // Only a bare keyword grant renders faithfully; a granted triggered/activated ability or a
                // parameterized keyword can't be reproduced here, so scaffold instead of dropping it.
                val kw = grantedKeyword(leObj) ?: return null
                inner.add("Effects.GrantKeyword(Keyword.$kw, $target$durArg)")
            }
            else -> return null  // any other layer effect (e.g. set base P/T, lose abilities) -> SCAFFOLD
        }
    }
    if (inner.isEmpty()) return null
    val effect = if (inner.size == 1) inner[0] else composite(inner)
    if (mass) {
        val gfArg = (node["args"].asArr)?.getOrNull(0) ?: JsonObject(emptyMap())
        val filter = groupFilterDsl(gfArg) ?: return null
        return "Effects.ForEachInGroup($filter, $effect)"
    }
    return effect
}

/**
 * The Onslaught Crowns' activated effect: "Enchanted creature and other creatures that share a creature
 * type with it get +P/+T / gain <keyword> / gain protection from <colors> until end of turn." mtgish
 * shapes this as a `CreateEachPermanentLayerEffectUntil` over the group `Or[HostPermanent,
 * And[Creature, SharesACreatureTypeWithPermanent(HostPermanent)]]` — a host-keyed group the generic
 * ForEachInGroup can't express. Recognise exactly that shape (+ end-of-turn) and render the dedicated
 * `GrantToEnchantedCreatureTypeGroupEffect`; anything else returns null so the generic path / scaffold runs.
 */
internal fun EmitCtx.enchantedTypeGroupGrant(node: JsonObject): String? {
    val args = node["args"].asArr ?: return null
    val blob = compact(args.getOrNull(0))
    if ("SharesACreatureTypeWithPermanent" !in blob || "HostPermanent" !in blob) return null
    if (firstExpiration(node) !in setOf(null, "UntilEndOfTurn")) return null  // non-EOT -> decline
    val layerEffects = (args.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>() ?: return null
    if (layerEffects.isEmpty()) return null
    val params = mutableListOf<String>()
    for (le in layerEffects) {
        when (le.strField("_LayerEffect")) {
            "AdjustPT" -> {
                val pt = le["args"].asArr ?: return null
                if (pt.size != 2) return null
                params.add("powerModifier = ${pt[0].asInt()}")
                params.add("toughnessModifier = ${pt[1].asInt()}")
            }
            "AddAbility" -> {
                val granted = (le["args"].asArr)?.getOrNull(0) as? JsonObject
                if (granted != null && jsonContains(granted, "_Protectable", "FromColor")) {
                    val colors = protectionGrantColors(granted) ?: return null
                    params.add("protectionColors = setOf(${colors.joinToString(", ") { "Color.$it" }})")
                } else {
                    val kw = grantedKeyword(le) ?: return null
                    params.add("keyword = Keyword.$kw")
                }
            }
            else -> return null
        }
    }
    if (params.isEmpty()) return null
    return "GrantToEnchantedCreatureTypeGroupEffect(${params.joinToString(", ")})"
}

/** The granted [Keyword] for an `AddAbility` layer effect, or null (-> SCAFFOLD) when the grant is not
 *  a single bare keyword — a granted triggered/activated ability or a parameterized keyword carries
 *  structure this faithful-keyword grant can't reproduce. */
private fun EmitCtx.grantedKeyword(addAbility: JsonObject): String? {
    val rule = (addAbility["args"].asArr)?.getOrNull(0) as? JsonObject ?: return null
    val rname = rule.strField("_Rule") ?: return null
    if (rname == "Landwalk") {  // AddAbility{Landwalk{Forest}} -> FORESTWALK
        val sub = subtypes(rule).firstOrNull()?.let { it.uppercase() + "WALK" }
        return if (sub != null && sub in keywords) sub else null
    }
    // A bare keyword rule carries no further structure. Anything with args (TriggerA, a parameterized
    // keyword, ...) is not a faithful bare-keyword grant.
    if (rule["args"] != null) return null
    val kw = pascalToUpperSnake(rname)
    return if (kw in keywords) kw else null
}

/** The layer effect's `_Expiration` -> "" for the default (end-of-turn) facade, an explicit
 *  `Duration.*` DSL for a recognised non-default duration, or null (-> SCAFFOLD) for one we can't
 *  render exactly (so the emitter never silently substitutes the wrong duration). */
private fun expirationDsl(node: JsonObject): String? =
    when (firstExpiration(node)) {
        null, "UntilEndOfTurn" -> ""
        "ForAsLongAsPermanentRemainsTapped" -> "Duration.WhileSourceTapped()"
        else -> null
    }

/** The first `_Expiration` discriminator value anywhere in the subtree. */
private fun firstExpiration(node: JsonElement?): String? {
    when (node) {
        is JsonObject -> {
            node.strField("_Expiration")?.let { return it }
            node.values.forEach { firstExpiration(it)?.let { v -> return v } }
        }
        is JsonArray -> node.forEach { firstExpiration(it)?.let { v -> return v } }
        else -> {}
    }
    return null
}
