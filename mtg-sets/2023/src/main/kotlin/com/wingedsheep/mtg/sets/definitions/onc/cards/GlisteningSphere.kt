package com.wingedsheep.mtg.sets.definitions.onc.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Glistening Sphere
 * {3}
 * Artifact
 * This artifact enters tapped.
 * When this artifact enters, proliferate.
 * {T}: Add one mana of any color.
 * Corrupted — {T}: Add three mana of any one color. Activate only if an opponent has three or
 * more poison counters.
 *
 * The corrupted mana ability is Gilded Lotus's `AddAnyColorMana(3)` behind the same
 * `OnlyIfCondition` activation restriction the Tainted lands use on their mana abilities.
 */
val GlisteningSphere = card("Glistening Sphere") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "This artifact enters tapped.\n" +
        "When this artifact enters, proliferate.\n" +
        "{T}: Add one mana of any color.\n" +
        "Corrupted — {T}: Add three mana of any one color. Activate only if an opponent has three or more poison counters."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Proliferate()
        description = "When this artifact enters, proliferate."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add one mana of any color."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(3)
        manaAbility = true
        timing = TimingRule.ManaAbility
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.Corrupted))
        description = "Corrupted — {T}: Add three mana of any one color. Activate only if an opponent has three or more poison counters."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "20"
        artist = "Tuan Duong Chu"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b825db05-7e95-400b-944b-526cfa6addc0.jpg?1783918158"
        ruling("2023-02-04", "You don't have to choose every permanent or player that has a counter, only the ones you want to add another counter to.")
    }
}
