package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Colossal Rattlewurm
 * {2}{G}{G}
 * Creature — Wurm
 * 6/5
 * Colossal Rattlewurm has flash as long as you control a Desert.
 * Trample
 * {1}{G}, Exile this card from your graveyard: Search your library for a Desert card,
 * put it onto the battlefield tapped, then shuffle.
 */
val ColossalRattlewurm = card("Colossal Rattlewurm") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wurm"
    power = 6
    toughness = 5
    oracleText = "Colossal Rattlewurm has flash as long as you control a Desert.\n" +
        "Trample\n" +
        "{1}{G}, Exile this card from your graveyard: Search your library for a Desert card, " +
        "put it onto the battlefield tapped, then shuffle."

    keywords(Keyword.TRAMPLE)

    conditionalFlash = Conditions.YouControl(GameObjectFilter.Land.withSubtype(Subtype.DESERT))

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{G}"), Costs.ExileSelf)
        activateFromZone = Zone.GRAVEYARD
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Land.withSubtype(Subtype.DESERT),
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "159"
        artist = "Filip Burburan"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/17a104d3-e4ac-44a0-9c6a-39965b1b9751.jpg?1712355903"
    }
}
