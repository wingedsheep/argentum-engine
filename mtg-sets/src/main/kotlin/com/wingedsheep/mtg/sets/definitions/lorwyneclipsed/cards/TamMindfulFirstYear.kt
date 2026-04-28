package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantHexproofFromOwnColorsToGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Tam, Mindful First-Year
 * {1}{G/U}
 * Legendary Creature — Gorgon Wizard
 * 2/2
 *
 * Each other creature you control has hexproof from each of its colors.
 * {T}: Target creature you control becomes all colors until end of turn.
 *
 * Per Wizards ruling (2025-11-17): "Hexproof from each color" is shorthand for hexproof from
 * white, from blue, from black, from red, and from green. Colorless is not a color.
 */
val TamMindfulFirstYear = card("Tam, Mindful First-Year") {
    manaCost = "{1}{G/U}"
    typeLine = "Legendary Creature — Gorgon Wizard"
    power = 2
    toughness = 2
    oracleText = "Each other creature you control has hexproof from each of its colors.\n" +
        "{T}: Target creature you control becomes all colors until end of turn."

    staticAbility {
        ability = GrantHexproofFromOwnColorsToGroup(
            filter = GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true)
        )
    }

    activatedAbility {
        cost = Costs.Tap
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.BecomeAllColors(creature)
        description = "{T}: Target creature you control becomes all colors until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "245"
        artist = "Zoltan Boros"
        flavorText = "\"What are the odds? And what would you like them to be?\""
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6cb0f825-b75b-4f2a-803c-08142ca07e76.jpg?1767862738"
        ruling("2025-11-17", "\"Hexproof from each color\" is shorthand for hexproof from white, from blue, from black, from red, and from green. Colorless is not a color.")
    }
}
