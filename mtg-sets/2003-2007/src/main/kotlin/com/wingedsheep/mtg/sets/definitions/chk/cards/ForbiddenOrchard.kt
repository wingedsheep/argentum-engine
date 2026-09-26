package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Forbidden Orchard — Champions of Kamigawa #276
 * Land
 *
 * {T}: Add one mana of any color.
 * Whenever you tap this land for mana, target opponent creates a 1/1 colorless Spirit creature
 * token.
 *
 * The trigger targets, so it is not a mana ability (CR 605.1b): it goes on the stack after the
 * mana is added and can be responded to. It fires however the land is tapped — a manual activation
 * or the auto-payer tapping it for a spell.
 */
val ForbiddenOrchard = card("Forbidden Orchard") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add one mana of any color.\n" +
        "Whenever you tap this land for mana, target opponent creates a 1/1 colorless Spirit creature token."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    triggeredAbility {
        trigger = Triggers.self.tappedForMana()
        val opponent = target(Targets.Opponent)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Spirit"),
            controller = opponent,
        )
        description = "Whenever you tap this land for mana, target opponent creates a 1/1 colorless Spirit creature token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "276"
        artist = "Dany Orizio"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88d78261-c8c9-4e0e-b157-f70ed46c3a25.jpg?1783944274"
    }
}
