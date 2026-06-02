package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DamageCantBePrevented
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCantBeCountered
import com.wingedsheep.sdk.scripting.events.DamageType

/**
 * Frenzied Baloth — {G}{G}
 * Creature — Beast
 * 3/2
 * This spell can't be countered.
 * Trample, haste
 * Creature spells you control can't be countered.
 * Combat damage can't be prevented.
 */
val FrenziedBaloth = card("Frenzied Baloth") {
    manaCost = "{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    oracleText = "This spell can't be countered.\nTrample, haste\nCreature spells you control can't be countered.\nCombat damage can't be prevented."
    power = 3
    toughness = 2

    cantBeCountered = true

    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    staticAbility {
        ability = GrantCantBeCountered(filter = GameObjectFilter.Creature)
    }

    replacementEffect(
        DamageCantBePrevented(appliesTo = EventPattern.DamageEvent(damageType = DamageType.Combat))
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "183"
        artist = "Diana Franco"
        flavorText = "Not all species on Evendo welcomed the thawing of the ice."
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c72d85e9-a0bc-4f73-8d73-c58843577f4e.jpg?1752947300"
    }
}
