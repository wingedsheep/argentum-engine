package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Transmogrant Altar
 * {3}
 * Artifact
 * {B}, {T}, Sacrifice a creature: Add {C}{C}{C}.
 * {2}, {T}, Sacrifice a creature: Create a 3/3 colorless Zombie artifact creature token.
 * Activate only as a sorcery.
 */
val TransmograntAltar = card("Transmogrant Altar") {
    manaCost = "{3}"
    colorIdentity = "B"
    typeLine = "Artifact"
    oracleText = "{B}, {T}, Sacrifice a creature: Add {C}{C}{C}.\n" +
        "{2}, {T}, Sacrifice a creature: Create a 3/3 colorless Zombie artifact creature token. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.Tap, Costs.Sacrifice(GameObjectFilter.Creature))
        effect = Effects.AddColorlessMana(3)
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{B}, {T}, Sacrifice a creature: Add {C}{C}{C}."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.Sacrifice(GameObjectFilter.Creature))
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            creatureTypes = setOf("Zombie"),
            artifactToken = true,
        )
        timing = TimingRule.SorcerySpeed
        description = "{2}, {T}, Sacrifice a creature: Create a 3/3 colorless Zombie artifact creature token. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "124"
        artist = "Dan Murayama Scott"
        flavorText = "\"The human body—like any other machine—can be stripped for parts.\"\n—Ashnod"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca28d210-a35d-491e-beda-76b95d09dc2d.jpg?1783920077"
    }
}
