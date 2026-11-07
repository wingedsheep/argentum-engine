package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.namedFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Secret Salvage — Aether Revolt #71
 * {3}{B}{B} · Sorcery · Rare
 *
 * Exile target nonland card from your graveyard. Search your library for any number of cards
 * with the same name as that card, reveal them, put them into your hand, then shuffle.
 *
 * The name is captured off the target before it is exiled (a card's name is the same in the
 * graveyard and in exile), then the library search matches it through
 * [GameObjectFilter.namedFromVariable] — Lobotomy's name-capture pipeline. "Any number" is an
 * unbounded pick that may be empty (CR 701.23b — a search may fail to find), and the search
 * event fires whether or not anything was found. If the target is gone at resolution the spell
 * has no legal target and does nothing, so no search happens.
 */
val SecretSalvage = card("Secret Salvage") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Exile target nonland card from your graveyard. Search your library for any number of " +
        "cards with the same name as that card, reveal them, put them into your hand, then shuffle."

    spell {
        target(TargetFilter(GameObjectFilter.Nonland.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.Pipeline {
            val salvaged = gather(CardSource.ChosenTargets)
            val salvagedName = storeCardName(salvaged)
            exile(salvaged)
            val searchable = gather(
                CardSource.FromZone(
                    Zone.LIBRARY,
                    Player.You,
                    GameObjectFilter.Any.namedFromVariable(salvagedName)
                ),
                search = true
            )
            val found = chooseAnyNumber(
                from = searchable,
                prompt = "Search your library for any number of cards with the same name as the exiled card",
                selectedLabel = "Put into your hand"
            )
            toHand(found, revealed = true)
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "71"
        artist = "Tommy Arnold"
        flavorText = "There is no shortage of illegal salvage in Ghirapur, nor of those who are willing to profit from it."
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9b9f3e2-b5e8-4234-aaf3-5f1938a20c78.jpg?1783936758"
    }
}
