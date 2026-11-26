package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Gem of Becoming — Magic 2013 #205
 * {3} · Artifact
 *
 * {3}, {T}, Sacrifice this artifact: Search your library for an Island card, a Swamp card, and a
 * Mountain card. Reveal those cards, put them into your hand, then shuffle.
 *
 * One search (one library-searched event), three "up to one" picks. Each pick chooses from the
 * previous pick's remainder, so a card with two of the types (Underground Sea) fills only one slot.
 * Finding nothing is allowed for any slot (the 2012-07-01 ruling).
 */
val GemOfBecoming = card("Gem of Becoming") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{3}, {T}, Sacrifice this artifact: Search your library for an Island card, a Swamp card, " +
        "and a Mountain card. Reveal those cards, put them into your hand, then shuffle."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.Pipeline {
            val library = gather(CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Any), search = true)
            val island = chooseUpToSplit(
                1, from = library,
                filter = GameObjectFilter.Any.withSubtype(Subtype.ISLAND),
                prompt = "Search your library for an Island card",
                selectedLabel = "Put into your hand"
            )
            val swamp = chooseUpToSplit(
                1, from = island.remainder,
                filter = GameObjectFilter.Any.withSubtype(Subtype.SWAMP),
                prompt = "Search your library for a Swamp card",
                selectedLabel = "Put into your hand"
            )
            val mountain = chooseUpTo(
                1, from = swamp.remainder,
                filter = GameObjectFilter.Any.withSubtype(Subtype.MOUNTAIN),
                prompt = "Search your library for a Mountain card",
                selectedLabel = "Put into your hand"
            )
            toHand(island.selected, revealed = true)
            toHand(swamp.selected, revealed = true)
            toHand(mountain, revealed = true)
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
        }
        description = "Search your library for an Island card, a Swamp card, and a Mountain card. " +
            "Reveal those cards, put them into your hand, then shuffle."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "205"
        artist = "Jack Wang"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e07bc36-2207-48f4-a151-4ccb0c6d851d.jpg?1783940464"
        ruling("2012-07-01", "You don't have to find all three cards, even if they're present in your library. For example, you can activate the ability just to find an Island card and a Swamp card.")
    }
}
