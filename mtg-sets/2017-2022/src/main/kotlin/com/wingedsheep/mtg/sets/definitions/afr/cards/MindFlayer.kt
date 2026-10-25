package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mind Flayer
 * {3}{U}{U}
 * Creature — Horror
 * 3/3
 * Dominate Monster — When this creature enters, gain control of target creature for as long as you
 * control this creature.
 *
 * Rangers of Ithilien's shape: the steal is gated by [Duration.WhileYouControlSource], so it ends
 * when Mind Flayer leaves or changes controller — and never takes hold if that already happened
 * before the trigger resolved.
 */
val MindFlayer = card("Mind Flayer") {
    manaCost = "{3}{U}{U}"
    typeLine = "Creature — Horror"
    power = 3
    toughness = 3
    oracleText = "Dominate Monster — When this creature enters, gain control of target creature for as long " +
        "as you control this creature."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.GainControl(creature, Duration.WhileYouControlSource("Mind Flayer"))
        description = "When this creature enters, gain control of target creature for as long as you " +
            "control this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "63"
        flavorText = "\"Shed your thoughts and let my will flow through you.\""
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c29ac797-a186-4224-94e1-9bb9dca4b029.jpg?1783926511"
        ruling("2021-07-23", "If Mind Flayer leaves the battlefield or you lose control of it before its Dominate Monster ability resolves, control of the target creature doesn't change at all.")
    }
}
