package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lightning Shrieker — Fate Reforged #106
 * {4}{R} · Creature — Dragon · 5/5
 * Flying, trample, haste
 * At the beginning of the end step, this creature's owner shuffles it into their library.
 *
 * "The end step" is every end step, so the trigger fires on any player's turn. A card moved to a
 * library always lands in its owner's, so a stolen Shrieker still goes home; `fromZone = BATTLEFIELD`
 * honours the ruling that it is shuffled away only if it is still on the battlefield on resolution.
 */
val LightningShrieker = card("Lightning Shrieker") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 5
    toughness = 5
    oracleText = "Flying, trample, haste\n" +
        "At the beginning of the end step, this creature's owner shuffles it into their library."

    keywords(Keyword.FLYING, Keyword.TRAMPLE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        effect = Effects.ShuffleIntoLibrary(EffectTarget.Self, fromZone = Zone.BATTLEFIELD)
        description = "At the beginning of the end step, this creature's owner shuffles it into their library."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "106"
        artist = "Slawomir Maniak"
        flavorText = "Dragonslayers learned to keep silent about their deeds after seeing the terrible " +
            "vengeance wrought by Kolaghan and her brood."
        imageUri = "https://cards.scryfall.io/normal/front/0/0/0071bf6e-a78b-4286-b3e7-acf44631a001.jpg?1783938687"
        ruling("2014-11-24", "Lightning Shrieker's owner shuffles it into their library only if it's on the battlefield as its ability resolves.")
    }
}
