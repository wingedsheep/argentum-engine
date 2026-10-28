package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Ondu Inversion {6}{W}{W} // Ondu Skyruins — Zendikar Rising #30
 * Sorcery // Land · Modal double-faced card
 *
 * Front — Ondu Inversion: "Destroy all nonland permanents." (Planar Cleansing's shape)
 * Back — Ondu Skyruins: "This land enters tapped. {T}: Add {W}."
 */
private val OnduInversionFront = card("Ondu Inversion") {
    manaCost = "{6}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Destroy all nonland permanents."

    spell {
        effect = Patterns.Group.destroyAllPipeline(GameObjectFilter.NonlandPermanent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "30"
        artist = "James Paick"
        flavorText = "\"At first rumble you'd best hold on tight and pray to any gods you still hold dear.\"\n—Nys Dal, Sea Gate trapfinder"
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b6e6be8c-41c3-4348-a8dd-b40ceb24e9b4.jpg?1783929416"
    }
}

private val OnduSkyruinsBack = card("Ondu Skyruins") {
    typeLine = "Land"
    colorIdentity = "W"
    oracleText = "This land enters tapped.\n{T}: Add {W}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "30"
        artist = "James Paick"
        flavorText = "\"It always seems so quiet in the aftermath of wrenching violence.\"\n—Nys Dal, Sea Gate trapfinder"
        imageUri = "https://cards.scryfall.io/normal/back/b/6/b6e6be8c-41c3-4348-a8dd-b40ceb24e9b4.jpg?1783929416"
    }
}

val OnduInversion: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = OnduInversionFront,
    backFace = OnduSkyruinsBack,
)
