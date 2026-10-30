package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Deathbellow War Cry
 * {5}{R}{R}{R}
 * Sorcery
 * Search your library for up to four Minotaur creature cards with different names, put them onto
 * the battlefield, then shuffle.
 *
 * Reach the Horizon's pipeline: gather the library's Minotaur creature cards → `chooseUpTo(4)` under
 * [SelectionRestriction.OnePerCardName] → onto the battlefield → shuffle. "Up to" lets the search
 * find fewer, or none.
 */
val DeathbellowWarCry = card("Deathbellow War Cry") {
    manaCost = "{5}{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Search your library for up to four Minotaur creature cards with different names, put them " +
        "onto the battlefield, then shuffle."

    spell {
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Creature.withSubtype("Minotaur")),
                search = true
            )
            val found = chooseUpTo(
                4,
                from = searchable,
                restrictions = listOf(SelectionRestriction.OnePerCardName),
                prompt = "Search for up to four Minotaur creature cards with different names"
            )
            move(found, CardDestination.ToZone(Zone.BATTLEFIELD))
            run(Effects.ShuffleLibrary())
            // CR 701.23 — the search happened whether or not anything was found.
            run(EmitLibrarySearchedEventEffect)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "294"
        artist = "Josh Hass"
        flavorText = "Deathbellow Canyon echoes with the roars of minotaurs calling each other to battle."
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf0f58ac-88ef-445c-a12b-0bb1cf482298.jpg?1783931490"
    }
}
