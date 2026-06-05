package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.compact
import com.wingedsheep.tooling.coverage.jsonContains
import com.wingedsheep.tooling.coverage.strField
import kotlinx.serialization.json.JsonObject

/**
 * Whole-card spell shortcuts — multi-action shapes recognised as one named `EffectPatterns.*` rather
 * than rendered action-by-action. [spellBlock] tries each before falling back to the generic path.
 * A `String?` shortcut yields a one-line `effect =`; a `List<String>?` shortcut yields a whole block.
 */
internal fun EmitCtx.eachplayerMaydraw(card: JsonObject): String? {
    val rules = card["Rules"]
    if (!jsonContains(rules, "_Action", "EachPlayerActions") || !jsonContains(rules, "_Action", "DrawUptoNumberCards")) return null
    val blob = compact(rules)
    val mx = Regex(""""DrawUptoNumberCards".*?"args":\s*(\d+)""").find(blob) ?: return null
    val life = Regex(""""GainLifeForEach".*?"args":\s*(\d+)""").find(blob)
    used.add("EffectPatterns")
    val lpc = if (life != null) ", lifePerCardNotDrawn = ${life.groupValues[1]}" else ""
    return "EffectPatterns.eachPlayerMayDraw(maxCards = ${mx.groupValues[1]}$lpc)"
}

/** Each player discards any number, then draws that many; you draw 1 (Flux). */
internal fun EmitCtx.fluxEffect(card: JsonObject): String? {
    val blob = compact(card["Rules"])
    if ("TheNumberOfCardsDiscardedByPlayerThisWay" in blob && "DiscardAnyNumberOfCards" in blob) {
        used.add("EffectPatterns")
        val bonus = if ("\"DrawACard\"" in blob) 1 else 0
        return "EffectPatterns.eachPlayerDiscardsDraws(controllerBonusDraw = $bonus)"
    }
    return null
}

/** Each player shuffles their hand into their library, then draws that many (Winds of Change). */
internal fun EmitCtx.windsEffect(card: JsonObject): String? {
    val blob = compact(card["Rules"])
    if ("ShuffleHandIntoLibrary" in blob && "NumCardsShuffledIntoLibraryThisWay" in blob) {
        used.addAll(listOf("EffectPatterns", "Player"))
        return "EffectPatterns.wheelEffect(Player.Each)"
    }
    return null
}

/** Take an extra turn, then lose at that turn's end step (Last Chance / Final Fortune). */
internal fun EmitCtx.extraTurnEffect(card: JsonObject): String? {
    val (_, actions) = extractEnvelope(card["Rules"])
    if (actions == null) return null
    val hasExtra = actions.any { it.strField("_Action") == "TakeAnExtraTurn" }
    val loseAfter = actions.any { it.strField("_Action") == "CreateFutureTrigger" && jsonContains(it, "_Action", "LoseTheGame") }
    if (hasExtra && loseAfter) { used.add("TakeExtraTurnEffect"); return "TakeExtraTurnEffect(loseAtEndStep = true)" }
    return null
}

/** Forked-Lightning shape: TargetedDistributed -> TargetCreature(count) + DividedDamageEffect. */
internal fun EmitCtx.distributedSpell(card: JsonObject): List<String>? {
    val blob = compact(card["Rules"])
    if ("\"TargetedDistributed\"" !in blob) return null
    val total = Regex(""""DistributeNumberAmongTargets","args":\{"_GameNumber":"Integer","args":(\d+)""").find(blob)
    val mx = Regex(""""BetweenOneAndNumberTargetPermanents","args":\[\{"_GameNumber":"Integer","args":(\d+)""").find(blob)
    if (total == null || mx == null) return null
    used.addAll(listOf("TargetCreature", "DividedDamageEffect"))
    val m = mx.groupValues[1]
    return listOf(
        "    spell {",
        "        target = TargetCreature(count = $m, minCount = 1)",
        "        effect = DividedDamageEffect(totalDamage = ${total.groupValues[1]}, minTargets = 1, maxTargets = $m)",
        "    }",
    )
}

/** Draw the difference between target opponent's hand and yours (Balance of Power). */
internal fun EmitCtx.balanceEffect(card: JsonObject): List<String>? {
    val blob = compact(card["Rules"])
    if ("NumCardsInHandIs" in blob && "\"Minus\"" in blob && "TheNumberOfCardsInPlayersHand" in blob) {
        used.addAll(listOf("DrawCardsEffect", "DynamicAmounts", "TargetOpponent"))
        return listOf(
            "    spell {",
            "        target = TargetOpponent()",
            "        effect = DrawCardsEffect(DynamicAmounts.handSizeDifferenceFromTargetOpponent())",
            "    }",
        )
    }
    return null
}
