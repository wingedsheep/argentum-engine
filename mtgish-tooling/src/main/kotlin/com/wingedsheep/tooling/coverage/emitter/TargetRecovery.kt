package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.asArr
import com.wingedsheep.tooling.coverage.compact
import com.wingedsheep.tooling.coverage.findInteger
import com.wingedsheep.tooling.coverage.jsonContains
import com.wingedsheep.tooling.coverage.strField
import com.wingedsheep.tooling.coverage.subtypes
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Target / filter recovery — reads the mtgish target vocabulary the coverage map discards and rebuilds
 * the Argentum target/filter DSL. A filter we can't faithfully render returns null → the card drops
 * to SCAFFOLD rather than emitting a confidently-wrong target.
 */
internal fun EmitCtx.creatureFilterDsl(filterNode: JsonElement?): String? {
    val blob = compact(filterNode)
    // "nonartifact creature" (the Terror template) renders via .nonartifact(); any OTHER non-cardtype
    // restriction has no faithful filter rendering yet, so drop to SCAFFOLD rather than omit it.
    val nonCardtypes = Regex(""""IsNonCardtype",\s*"args":\s*"(\w+)"""").findAll(blob).map { it.groupValues[1] }.toList()
    if (nonCardtypes.any { it != "Artifact" }) return null
    // Whole-creature shapes whose helpers live on GameObjectFilter (not TargetFilter), or are a named
    // TargetFilter constant. ONS targets use these in isolation, so render them as the whole filter.
    if ("IsAttacking" in blob && "IsBlocking" in blob) {
        // "...with flying" composes onto the attacking-or-blocking base (Venomspout Brackus).
        return if ("\"Flying\"" in blob) "TargetFilter(GameObjectFilter.Creature.attackingOrBlocking().withKeyword(Keyword.FLYING))"
        else "TargetFilter.AttackingOrBlockingCreature"
    }
    if ("IsFaceDown" in blob) return "TargetFilter(GameObjectFilter.Creature.faceDown())"
    // "Goblin creature" / "Elf or Soldier creature": one subtype -> withSubtype; several -> an Or of
    // per-subtype creature filters (matches golden's distributed Or[And[IsCreature, HasSubtype X]…]).
    val subs = Regex(""""IsCreatureType",\s*"args":\s*"(\w+)"""").findAll(blob).map { it.groupValues[1] }.toList()
    if (subs.isNotEmpty()) {
        return "TargetFilter(${subs.joinToString(" or ") { "GameObjectFilter.Creature.withSubtype(\"$it\")" }})"
    }
    var suffix = ""
    if ("Artifact" in nonCardtypes) suffix += ".nonartifact()"
    Regex(""""IsNonColor".*?"_Color":\s*"(\w+)"""").find(blob)?.let {
        suffix += ".notColor(Color.${it.groupValues[1].uppercase()})"
    }
    Regex(""""IsColor".*?"_Color":\s*"(\w+)"""").find(blob)?.let {
        suffix += ".withColor(Color.${it.groupValues[1].uppercase()})"
    }
    if (jsonContains(filterNode, "_Permanents", "DoesntHaveAbility") && "\"Flying\"" in blob) {
        suffix += ".withoutKeyword(Keyword.FLYING)"
    }
    if ("IsTapped" in blob) suffix += ".tapped()"
    if ("IsAttacking" in blob) suffix += ".attacking()"
    Regex(""""PowerIs".*?"LessThanOrEqualTo".*?"Integer",\s*"args":\s*(\d+)""").find(blob)?.let {
        suffix += ".powerAtMost(${it.groupValues[1]})"
    }
    return "TargetFilter.Creature$suffix"
}

private fun targetTypes(args: JsonElement?): Set<String> =
    Regex(""""IsCardtype",\s*"args":\s*"(\w+)"""").findAll(compact(args)).map { it.groupValues[1] }.toSet()

/** Faithful Argentum target DSL, or null if the filter can't be rendered (-> not AUTO). */
internal fun EmitCtx.targetDsl(tnode: JsonObject, actionContext: List<JsonObject>? = null): String? {
    val ttype = tnode.strField("_Target")
    val args = tnode["args"]
    val countInt = findInteger(tnode)
    if (ttype == "TargetPlayer") {
        return if (jsonContains(tnode, "_Players", "Opponent")) "TargetOpponent()" else "TargetPlayer()"
    }
    if (ttype == "AnyTarget" || ttype == "TargetPlayerOrPermanent") {
        val blob = compact(tnode)
        if ("Planeswalker" in blob && "Player" in blob && "Opponent" in blob) return "TargetOpponentOrPlaneswalker()"
        if ("Planeswalker" in blob && "Player" in blob) return "TargetPlayerOrPlaneswalker()"
        if ("Planeswalker" in blob && "Creature" in blob) return "TargetCreatureOrPlaneswalker()"
        if (actionContext != null && actionContext.consumesOnlyTargetPlayer()) return "TargetPlayer()"
        return "AnyTarget()"
    }
    if (ttype in setOf("TargetPermanent", "NumberTargetPermanents", "UptoNumberTargetPermanents", "OneOrTwoTargetPermanents")) {
        val types = targetTypes(args)
        val blob = compact(args)
        // A creature-subtype restriction ("target Wall") implies a creature target even with no explicit
        // IsCardtype Creature; route it through the creature filter so the subtype isn't dropped (Tunnel).
        val creatureTarget = types == setOf("Creature") || (types.isEmpty() && "IsCreatureType" in blob)
        if (creatureTarget) {
            val filter = creatureFilterDsl(args) ?: return null
            val parts = mutableListOf("filter = $filter")
            if (ttype in setOf("NumberTargetPermanents", "UptoNumberTargetPermanents") && countInt is Int) parts.add(0, "count = $countInt")
            if (ttype == "OneOrTwoTargetPermanents") { parts.add(0, "minCount = 1"); parts.add(0, "count = 2") }
            if (ttype == "UptoNumberTargetPermanents") parts.add(0, "optional = true")
            return "TargetCreature(${parts.joinToString(", ")})"
        }
        val singleType = mapOf("Land" to "TargetFilter.Land", "Artifact" to "TargetFilter.Artifact", "Enchantment" to "TargetFilter.Enchantment")
        if (types.size == 1 && types.first() in singleType) {
            val parts = mutableListOf("filter = ${singleType[types.first()]}")
            if (ttype in setOf("NumberTargetPermanents", "UptoNumberTargetPermanents") && countInt is Int) parts.add(0, "count = $countInt")
            if (ttype == "UptoNumberTargetPermanents") parts.add(0, "optional = true")
            return "TargetPermanent(${parts.joinToString(", ")})"
        }
        if (types.isEmpty() && "IsCardtype" !in blob && "IsCreatureType" !in blob) {
            return "TargetPermanent()"
        }
        val multiType = mapOf(
            setOf("Creature", "Land") to "TargetFilter.CreatureOrLandPermanent",
            setOf("Creature", "Artifact") to "TargetFilter.CreatureOrArtifact",
            setOf("Creature", "Enchantment") to "TargetFilter.CreatureOrEnchantment",
            setOf("Artifact", "Enchantment") to "TargetFilter.ArtifactOrEnchantment",
        )
        multiType[types]?.let {
            return "TargetPermanent(filter = $it)"
        }
        return null  // unusual filters: not rendered yet -> SCAFFOLD
    }
    if (ttype == "TargetSpell") {
        val types = targetTypes(args)
        if (types == setOf("Creature", "Sorcery")) return "TargetSpell(filter = TargetFilter.CreatureOrSorcerySpellOnStack)"
        if (types == setOf("Instant", "Sorcery")) return "TargetSpell(filter = TargetFilter.InstantOrSorcerySpellOnStack)"
        if (types == setOf("Creature")) return "TargetSpell(filter = TargetFilter.CreatureSpellOnStack)"
        if (types.isEmpty()) return "TargetSpell()"
        return null
    }
    if (ttype == "TargetGraveyardCard") {
        val blob = compact(args)
        val types = targetTypes(args)
        val filt = when {
            types.isEmpty() && "IsCardtype" !in blob -> "TargetFilter.CardInGraveyard"
            types == setOf("Creature") -> {
                if ("\"You\"" in blob) "TargetFilter.CreatureInYourGraveyard" else "TargetFilter.CreatureInGraveyard"
            }
            types == setOf("Instant", "Sorcery") -> graveyardFilter("InstantOrSorcery", blob)
            types.size == 1 && types.first() in graveyardSingleTypeFilters -> graveyardFilter(graveyardSingleTypeFilters.getValue(types.first()), blob)
            else -> return null
        }
        return "TargetObject(filter = $filt)"
    }
    return null
}

private val graveyardSingleTypeFilters = mapOf(
    "Artifact" to "Artifact",
    "Enchantment" to "Enchantment",
    "Instant" to "Instant",
    "Land" to "Land",
    "Sorcery" to "Sorcery",
)

private fun graveyardFilter(gameObjectFilter: String, blob: String): String {
    val owner = when {
        "\"You\"" in blob -> ".ownedByYou()"
        "\"Opponent\"" in blob -> ".ownedByOpponent()"
        else -> ""
    }
    return "TargetFilter(GameObjectFilter.$gameObjectFilter$owner, zone = Zone.GRAVEYARD)"
}

private fun List<JsonObject>.consumesOnlyTargetPlayer(): Boolean {
    val targetPlayer = any { jsonContains(it, "_Player", "Ref_TargetPlayer") }
    val targetPermanent = any { jsonContains(it, "_Permanent", "Ref_TargetPermanent") }
    val targetGraveyardCard = any { jsonContains(it, "_GraveyardCard", "Ref_TargetGraveyardCard") }
    return targetPlayer && !targetPermanent && !targetGraveyardCard
}

/** GroupFilter for mass effects. If we can't preserve the filter, scaffold rather than widen. */
internal fun EmitCtx.groupFilterDsl(filterNode: JsonElement?): String? {
    val filtered = gameObjectFilterDsl(filterNode) ?: return null
    val oracle = oracleText?.lowercase() ?: ""
    val args = mutableListOf(filtered)
    // The IR's `Other(ThisPermanent)` is the authoritative "excludeSelf" signal; the oracle phrasing
    // ("all other" / "each other" / "other ... creatures") is the fallback for shapes without it.
    if (jsonContains(filterNode, "_Permanents", "Other") ||
        "all other" in oracle || "each other" in oracle) args.add("excludeSelf = true")
    return "GroupFilter(${args.joinToString(", ")})"
}

internal fun EmitCtx.gameObjectFilterDsl(filterNode: JsonElement?): String? {
    val blob = compact(filterNode)
    val types = targetTypes(filterNode)
    val subs = subtypes(filterNode)
    // Creature subtypes come from IsCreatureType (subtypes() only collects land/card subtypes).
    val creatureSubs = Regex(""""IsCreatureType",\s*"args":\s*"(\w+)"""").findAll(blob).map { it.groupValues[1] }.toList()
    var filtered = when {
        subs.isNotEmpty() && ("Land" in types || "IsLandType" in blob || "\"Land\"" in blob) ->
            "GameObjectFilter.Land.withSubtype(${subtypeArg(subs[0])})"
        // A creature subtype always implies creature, so render Creature.withSubtype even when there's no
        // explicit IsCardtype Creature (the "other Merfolk"/"other Goblins" lord groups) — otherwise the
        // ThisPermanent marker below would wrongly widen it to GameObjectFilter.Permanent.
        creatureSubs.isNotEmpty() ->
            "GameObjectFilter.Creature.withSubtype(${subtypeArg(creatureSubs[0])})"
        subs.isNotEmpty() && ("Creature" in types || "\"Creature\"" in blob) ->
            "GameObjectFilter.Creature.withSubtype(${subtypeArg(subs[0])})"
        types == setOf("Creature", "Land") -> "GameObjectFilter.CreatureOrLand"
        types == setOf("Creature", "Artifact") -> "GameObjectFilter.CreatureOrArtifact"
        types == setOf("Creature", "Enchantment") -> "GameObjectFilter.CreatureOrEnchantment"
        types == setOf("Artifact", "Enchantment") -> "GameObjectFilter.ArtifactOrEnchantment"
        "Creature" in types || "\"Creature\"" in blob -> "GameObjectFilter.Creature"
        "Land" in types || "\"Land\"" in blob -> "GameObjectFilter.Land"
        "Artifact" in types || "\"Artifact\"" in blob -> "GameObjectFilter.Artifact"
        "Enchantment" in types || "\"Enchantment\"" in blob -> "GameObjectFilter.Enchantment"
        "Permanent" in blob -> "GameObjectFilter.Permanent"
        else -> return null
    }
    val colors = Regex(""""_Color":\s*"(\w+)"""").findAll(blob).map { it.groupValues[1].uppercase() }.distinct().toList()
    if (colors.size > 1 && "\"Or\"" in blob) {
        filtered += ".withAnyColor(${colors.joinToString(", ") { "Color.$it" }})"
    } else if (colors.size == 1) {
        filtered += if ("IsNonColor" in blob) ".notColor(Color.${colors[0]})" else ".withColor(Color.${colors[0]})"
    }
    if (jsonContains(filterNode, "_Permanents", "DoesntHaveAbility") && "\"Flying\"" in blob) {
        filtered += ".withoutKeyword(Keyword.FLYING)"
    } else if ("\"Flying\"" in blob) {
        filtered += ".withKeyword(Keyword.FLYING)"
    }
    Regex(""""PowerIs".*?"GreaterThanOrEqualTo".*?"Integer","args":(\d+)""").find(blob)?.let {
        filtered += ".powerAtLeast(${it.groupValues[1]})"
    }
    Regex(""""PowerIs".*?"LessThanOrEqualTo".*?"Integer","args":(\d+)""").find(blob)?.let {
        filtered += ".powerAtMost(${it.groupValues[1]})"
    }
    if ("IsTapped" in blob) filtered += ".tapped()"
    if ("IsUntapped" in blob) filtered += ".untapped()"
    if ("IsAttacking" in blob) filtered += ".attacking()"
    if ("\"You\"" in blob) filtered += ".youControl()"
    if ("\"Opponent\"" in blob) filtered += ".opponentControls()"
    return filtered
}

internal fun EmitCtx.revealedHandFilterDsl(filterNode: JsonElement?): String? {
    val blob = compact(filterNode)
    val landType = Regex(""""IsLandType","args":"([^"]+)"""").find(blob)?.groupValues?.get(1)
    val color = Regex(""""_Color":"(\w+)"""").find(blob)?.groupValues?.get(1)
    if (landType == null && color == null) return null
    val parts = mutableListOf<String>()
    if (landType != null) parts.add("GameObjectFilter.Land.withSubtype(${subtypeArg(landType)})")
    if (color != null) parts.add("GameObjectFilter.Any.withColor(Color.${color.uppercase()})")
    return parts.joinToString(" or ").let { if (parts.size > 1) "($it)" else it }
}

internal fun EmitCtx.landSearchFilterDsl(filterNode: JsonElement?): String {
    val subs = subtypes(filterNode)
    // Dual-land fetch ("a Swamp or Mountain card") -> Land + Or[HasSubtype…], i.e. withAnySubtype;
    // golden factors IsLand out (unlike the distributed creature-subtype form).
    if (subs.size >= 2) return "GameObjectFilter.Land.withAnySubtype(${subs.joinToString(", ") { "\"$it\"" }})"
    if (subs.isNotEmpty()) return "GameObjectFilter.Land.withSubtype(${subtypeArg(subs[0])})"
    val blob = compact(filterNode)
    val oracle = oracleText?.lowercase() ?: ""
    return when {
        "basic land" in oracle || "IsBasicLand" in blob -> "GameObjectFilter.BasicLand"
        "sorcery card" in oracle || "\"Sorcery\"" in blob -> "GameObjectFilter.Sorcery"
        "instant card" in oracle || "\"Instant\"" in blob -> "GameObjectFilter.Instant"
        "\"Land\"" in blob -> "GameObjectFilter.Land"
        "\"Creature\"" in blob || "creature" in oracle -> {
            var out = "GameObjectFilter.Creature"
            if ("black creature" in oracle) out += ".withColor(Color.BLACK)"
            else Regex(""""_Color":\s*"(\w+)"""").find(blob)?.let { out += ".withColor(Color.${it.groupValues[1].uppercase()})" }
            if ("tapped creature" in oracle) out += ".tapped()"
            if ("attacking" in oracle) out += ".attacking()"
            out
        }
        else -> "GameObjectFilter.Any"
    }
}
