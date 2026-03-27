package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersWithCreatureTypeChoice
import com.wingedsheep.sdk.scripting.ModifyStatsForChosenCreatureType
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaEffect

/**
 * Patchwork Banner
 * {3}
 * Artifact
 *
 * As this artifact enters, choose a creature type.
 * Creatures you control of the chosen type get +1/+1.
 * {T}: Add one mana of any color.
 */
val PatchworkBanner = card("Patchwork Banner") {
    manaCost = "{3}"
    typeLine = "Artifact"
    oracleText = "As this artifact enters, choose a creature type.\nCreatures you control of the chosen type get +1/+1.\n{T}: Add one mana of any color."

    replacementEffect(EntersWithCreatureTypeChoice())

    staticAbility {
        ability = ModifyStatsForChosenCreatureType(
            powerBonus = 1,
            toughnessBonus = 1,
            youControlOnly = true
        )
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddAnyColorManaEffect(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "247"
        artist = "Sarah Finnigan"
        flavorText = "With thistle, quill, twig, and beak, the finest artisans of each animalfolk society stitched unique quilts, patched together as a symbol of harmony."
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8a982c8-bc08-44ba-b3ed-9e4b124615d6.jpg?1721427285"
    }
}
