package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Voracious Wurm
 * {1}{G}
 * Creature — Wurm
 * 2/2
 * This creature enters with X +1/+1 counters on it, where X is the amount of life you've gained this turn.
 *
 * An [EntersWithDynamicCounters] replacement (CR 614.1c) reading the controller's life-gained-this-turn
 * tally — the amount gained, not the net change in life total (see rulings).
 */
val VoraciousWurm = card("Voracious Wurm") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Wurm"
    power = 2
    toughness = 2
    oracleText = "This creature enters with X +1/+1 counters on it, where X is the amount of life you've gained this turn."

    replacementEffect(EntersWithDynamicCounters(count = DynamicAmounts.lifeGainedThisTurn(Player.You)))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "200"
        artist = "Igor Kieryluk"
        flavorText = "Shepherds in Kalonia know it's better to let sheep roam than have them be trapped when the wurms are feeding."
        imageUri = "https://cards.scryfall.io/normal/front/d/a/da15100b-2934-438c-9917-84ad8bdc4181.jpg?1783939898"
        ruling("2013-07-01", "Voracious Wurm's ability checks how much life you've gained during the turn, not what your life total is compared to what it was when the turn began. For example, if you start the turn at 10 life, gain 6 life during the turn, then lose 6 life later that turn, Voracious Wurm will enter the battlefield with six +1/+1 counters.")
        ruling("2013-07-01", "In a Two-Headed Giant game, life gained by your teammate isn't considered, even though it causes your team's life total to increase.")
    }
}
