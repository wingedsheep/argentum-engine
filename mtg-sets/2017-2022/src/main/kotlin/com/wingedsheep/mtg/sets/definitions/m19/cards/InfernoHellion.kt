package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Inferno Hellion — Core Set 2019 #148
 * {3}{R} · Creature — Hellion · 7/3
 * Trample
 * At the beginning of each end step, if this creature attacked or blocked this turn, its owner
 * shuffles it into their library.
 *
 * The intervening-if reads the turn-scoped attack/block markers, so a creature that fought in
 * combat is caught at any player's end step. `fromZone = BATTLEFIELD` honours the ruling: it is
 * shuffled away only if it is still on the battlefield as the ability resolves.
 */
val InfernoHellion = card("Inferno Hellion") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Hellion"
    power = 7
    toughness = 3
    oracleText = "Trample (This creature can deal excess combat damage to the player or planeswalker it's attacking.)\n" +
        "At the beginning of each end step, if this creature attacked or blocked this turn, its owner shuffles it into their library."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        interveningIf = Conditions.SourceAttackedOrBlockedThisTurn
        effect = Effects.ShuffleIntoLibrary(EffectTarget.Self, fromZone = Zone.BATTLEFIELD)
        description = "At the beginning of each end step, if this creature attacked or blocked this turn, its owner shuffles it into their library."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "148"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9cd11004-5b8f-4c71-bde0-eb75eed869b0.jpg?1783934549"
        ruling("2018-07-13", "Inferno Hellion's owner shuffles it into their library only if it's on the battlefield as its last ability resolves.")
    }
}
