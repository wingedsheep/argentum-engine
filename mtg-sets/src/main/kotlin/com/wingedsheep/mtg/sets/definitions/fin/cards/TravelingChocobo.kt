package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalETBOrLTBTriggers
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.PlayLandsAndCastFilteredFromTopOfLibrary

/**
 * Traveling Chocobo
 * {2}{G}
 * Creature — Bird
 * 3/2
 * You may look at the top card of your library any time.
 * You may play lands and cast Bird spells from the top of your library.
 * If a land or Bird you control entering the battlefield causes a triggered ability
 * of a permanent you control to trigger, that ability triggers an additional time.
 */
val TravelingChocobo = card("Traveling Chocobo") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Bird"
    power = 3
    toughness = 2
    oracleText = "You may look at the top card of your library any time.\nYou may play lands and cast Bird spells from the top of your library.\nIf a land or Bird you control entering the battlefield causes a triggered ability of a permanent you control to trigger, that ability triggers an additional time."

    staticAbility {
        ability = LookAtTopOfLibrary
    }

    staticAbility {
        ability = PlayLandsAndCastFilteredFromTopOfLibrary(
            spellFilter = GameObjectFilter.Creature.withSubtype("Bird")
        )
    }

    staticAbility {
        ability = AdditionalETBOrLTBTriggers(
            filter = GameObjectFilter.Land or GameObjectFilter.Creature.withSubtype("Bird"),
            description = "If a land or Bird you control entering the battlefield causes a triggered ability of a permanent you control to trigger, that ability triggers an additional time"
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "210"
        artist = "Ashley Mackenzie"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/2462df62-fc35-47ed-9571-40452074dc6d.jpg?1748706549"
    }
}
