package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kragma Butcher
 * {2}{R}
 * Creature — Minotaur Warrior
 * 2/3
 *
 * Inspired — Whenever this creature becomes untapped, it gets +2/+0 until end of turn.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val KragmaButcher = card("Kragma Butcher") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Minotaur Warrior"
    power = 2
    toughness = 3
    oracleText = "Inspired — Whenever this creature becomes untapped, it gets +2/+0 until end of turn."

    triggeredAbility {
        trigger = Triggers.self.becomesUntapped()
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "100"
        artist = "Daarken"
        flavorText = "Minotaurs go into battle hungry. The first sight of their enemies' blood sends them into a flesh-eating rage."
        imageUri = "https://cards.scryfall.io/normal/front/5/6/56d1dcab-7e46-4ff1-91f6-77352ef0cd90.jpg?1783939545"
    }
}
