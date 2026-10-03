package com.wingedsheep.mtg.sets.definitions.jmp.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Brightmare
 * {2}{W}
 * Creature — Unicorn
 * 2/3
 *
 * When this creature enters, tap up to one target creature. You gain life equal to that
 * creature's power.
 *
 * The life gain reads the target's power live (tapping doesn't change it); with no target chosen
 * the amount reads nothing and no life is gained.
 */
val Brightmare = card("Brightmare") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Unicorn"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, tap up to one target creature. You gain life equal to that creature's power."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.Tap(creature) then
            Effects.GainLife(DynamicAmounts.powerOf(creature))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "2"
        artist = "Steven Belledin"
        flavorText = "A ray of hope in the darkest night."
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0fc18921-59f5-413f-a221-dc47d31b2ec8.jpg"
    }
}
