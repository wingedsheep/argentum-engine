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
internal fun EmitCtx.creatureFilterDsl(filterNode: JsonElement?): String {
    used.add("TargetFilter")
    var suffix = ""
    val blob = compact(filterNode)
    Regex(""""IsNonColor".*?"_Color":\s*"(\w+)"""").find(blob)?.let {
        used.add("Color"); suffix += ".notColor(Color.${it.groupValues[1].uppercase()})"
    }
    Regex(""""IsColor".*?"_Color":\s*"(\w+)"""").find(blob)?.let {
        used.add("Color"); suffix += ".color(Color.${it.groupValues[1].uppercase()})"
    }
    if (jsonContains(filterNode, "_Permanents", "DoesntHaveAbility") && "\"Flying\"" in blob) {
        used.add("Keyword"); suffix += ".withoutKeyword(Keyword.FLYING)"
    }
    if ("IsTapped" in blob) suffix += ".tapped()"
    if ("IsAttacking" in blob) suffix += ".attacking()"
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
        return if (jsonContains(tnode, "_Players", "Opponent")) {
            used.add("TargetOpponent"); "TargetOpponent()"
        } else {
            used.add("TargetPlayer"); "TargetPlayer()"
        }
    }
    if (ttype == "AnyTarget" || ttype == "TargetPlayerOrPermanent") {
        val blob = compact(tnode)
        if ("Planeswalker" in blob && "Player" in blob && "Opponent" in blob) {
            used.add("TargetOpponentOrPlaneswalker")
            return "TargetOpponentOrPlaneswalker()"
        }
        if ("Planeswalker" in blob && "Player" in blob) {
            used.add("TargetPlayerOrPlaneswalker")
            return "TargetPlayerOrPlaneswalker()"
        }
        if ("Planeswalker" in blob && "Creature" in blob) {
            used.add("TargetCreatureOrPlaneswalker")
            return "TargetCreatureOrPlaneswalker()"
        }
        if (actionContext != null && actionContext.consumesOnlyTargetPlayer()) {
            used.add("TargetPlayer")
            return "TargetPlayer()"
        }
        used.add("AnyTarget"); return "AnyTarget()"
    }
    if (ttype in setOf("TargetPermanent", "NumberTargetPermanents", "UptoNumberTargetPermanents", "OneOrTwoTargetPermanents")) {
        val types = targetTypes(args)
        if (types == setOf("Creature")) {
            used.add("TargetCreature")
            val parts = mutableListOf("filter = ${creatureFilterDsl(args)}")
            if (ttype in setOf("NumberTargetPermanents", "UptoNumberTargetPermanents") && countInt is Int) parts.add(0, "count = $countInt")
            if (ttype == "OneOrTwoTargetPermanents") { parts.add(0, "minCount = 1"); parts.add(0, "count = 2") }
            if (ttype == "UptoNumberTargetPermanents") parts.add(0, "optional = true")
            return "TargetCreature(${parts.joinToString(", ")})"
        }
        val singleType = mapOf("Land" to "TargetFilter.Land", "Artifact" to "TargetFilter.Artifact", "Enchantment" to "TargetFilter.Enchantment")
        if (types.size == 1 && types.first() in singleType) {
            used.addAll(listOf("TargetPermanent", "TargetFilter"))
            val parts = mutableListOf("filter = ${singleType[types.first()]}")
            if (ttype in setOf("NumberTargetPermanents", "UptoNumberTargetPermanents") && countInt is Int) parts.add(0, "count = $countInt")
            if (ttype == "UptoNumberTargetPermanents") parts.add(0, "optional = true")
            return "TargetPermanent(${parts.joinToString(", ")})"
        }
        if (types.isEmpty() && "IsCardtype" !in compact(args)) {
            used.add("TargetPermanent"); return "TargetPermanent()"
        }
        val multiType = mapOf(
            setOf("Creature", "Land") to "TargetFilter.CreatureOrLandPermanent",
            setOf("Creature", "Artifact") to "TargetFilter.CreatureOrArtifact",
            setOf("Creature", "Enchantment") to "TargetFilter.CreatureOrEnchantment",
            setOf("Artifact", "Enchantment") to "TargetFilter.ArtifactOrEnchantment",
        )
        multiType[types]?.let {
            used.addAll(listOf("TargetPermanent", "TargetFilter"))
            return "TargetPermanent(filter = $it)"
        }
        return null  // unusual filters: not rendered yet -> SCAFFOLD
    }
    if (ttype == "TargetSpell") {
        val types = targetTypes(args)
        if (types == setOf("Creature", "Sorcery")) {
            used.addAll(listOf("TargetSpell", "TargetFilter"))
            return "TargetSpell(filter = TargetFilter.CreatureOrSorcerySpellOnStack)"
        }
        if (types == setOf("Instant", "Sorcery")) {
            used.addAll(listOf("TargetSpell", "TargetFilter"))
            return "TargetSpell(filter = TargetFilter.InstantOrSorcerySpellOnStack)"
        }
        if (types == setOf("Creature")) {
            used.addAll(listOf("TargetSpell", "TargetFilter"))
            return "TargetSpell(filter = TargetFilter.CreatureSpellOnStack)"
        }
        if (types.isEmpty()) { used.add("TargetSpell"); return "TargetSpell()" }
        return null
    }
    if (ttype == "TargetGraveyardCard") {
        used.addAll(listOf("TargetObject", "TargetFilter"))
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

private fun EmitCtx.graveyardFilter(gameObjectFilter: String, blob: String): String {
    used.addAll(listOf("GameObjectFilter", "Zone"))
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
    used.addAll(listOf("GroupFilter", "GameObjectFilter"))
    val filtered = gameObjectFilterDsl(filterNode) ?: return null
    val blob = compact(filterNode)
    val oracle = oracleText?.lowercase() ?: ""
    val args = mutableListOf(filtered)
    if ("all other" in oracle || "each other" in oracle) args.add("excludeSelf = true")
    return "GroupFilter(${args.joinToString(", ")})"
}

internal fun EmitCtx.gameObjectFilterDsl(filterNode: JsonElement?): String? {
    used.add("GameObjectFilter")
    val blob = compact(filterNode)
    val types = targetTypes(filterNode)
    val subs = subtypes(filterNode)
    var filtered = when {
        subs.isNotEmpty() && ("Land" in types || "IsLandType" in blob || "\"Land\"" in blob) ->
            "GameObjectFilter.Land.withSubtype(\"${subs[0]}\")"
        subs.isNotEmpty() && ("Creature" in types || "\"Creature\"" in blob) ->
            "GameObjectFilter.Creature.withSubtype(\"${subs[0]}\")"
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
        used.add("Color")
        filtered += ".withAnyColor(${colors.joinToString(", ") { "Color.$it" }})"
    } else if (colors.size == 1) {
        used.add("Color")
        filtered += if ("IsNonColor" in blob) ".notColor(Color.${colors[0]})" else ".withColor(Color.${colors[0]})"
    }
    if (jsonContains(filterNode, "_Permanents", "DoesntHaveAbility") && "\"Flying\"" in blob) {
        used.add("Keyword")
        filtered += ".withoutKeyword(Keyword.FLYING)"
    } else if ("\"Flying\"" in blob) {
        used.add("Keyword")
        filtered += ".withKeyword(Keyword.FLYING)"
    }
    Regex(""""PowerIs".*?"GreaterThanOrEqualTo".*?"Integer","args":(\d+)""").find(blob)?.let {
        filtered += ".powerAtLeast(${it.groupValues[1]})"
    }
    if ("IsTapped" in blob) filtered += ".tapped()"
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
    used.add("GameObjectFilter")
    val parts = mutableListOf<String>()
    if (landType != null) parts.add("GameObjectFilter.Land.withSubtype(\"$landType\")")
    if (color != null) { used.add("Color"); parts.add("GameObjectFilter.Any.withColor(Color.${color.uppercase()})") }
    return parts.joinToString(" or ").let { if (parts.size > 1) "($it)" else it }
}

internal fun EmitCtx.landSearchFilterDsl(filterNode: JsonElement?): String {
    used.add("GameObjectFilter")
    val subs = subtypes(filterNode)
    if (subs.isNotEmpty()) return "GameObjectFilter.Land.withSubtype(\"${subs[0]}\")"
    val blob = compact(filterNode)
    val oracle = oracleText?.lowercase() ?: ""
    return when {
        "basic land" in oracle || "IsBasicLand" in blob -> "GameObjectFilter.BasicLand"
        "sorcery card" in oracle || "\"Sorcery\"" in blob -> "GameObjectFilter.Sorcery"
        "instant card" in oracle || "\"Instant\"" in blob -> "GameObjectFilter.Instant"
        "\"Land\"" in blob -> "GameObjectFilter.Land"
        "\"Creature\"" in blob || "creature" in oracle -> {
            var out = "GameObjectFilter.Creature"
            if ("black creature" in oracle) { used.add("Color"); out += ".withColor(Color.BLACK)" }
            else Regex(""""_Color":\s*"(\w+)"""").find(blob)?.let { used.add("Color"); out += ".withColor(Color.${it.groupValues[1].uppercase()})" }
            if ("tapped creature" in oracle) out += ".tapped()"
            if ("attacking" in oracle) out += ".attacking()"
            out
        }
        else -> "GameObjectFilter.Any"
    }
}
