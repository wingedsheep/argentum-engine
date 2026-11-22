package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Sire of Insanity
 * {4}{B}{R}
 * Creature — Demon
 * 6/4
 * At the beginning of each end step, each player discards their hand.
 */
val SireOfInsanity = card("Sire of Insanity") {
    manaCost = "{4}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Demon"
    power = 6
    toughness = 4
    oracleText = "At the beginning of each end step, each player discards their hand."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        effect = Effects.ForEachPlayer(Player.Each, Patterns.Hand.discardHand())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "104"
        artist = "Peter Mohrbacher"
        flavorText = "Its victims become mindless lunatics. Conveniently, that's the first step in joining the Cult of Rakdos."
        imageUri = "https://cards.scryfall.io/normal/front/3/6/3665cfb7-51b6-4083-8eae-fbd3fa6c3554.jpg?1783940021"
    }
}
