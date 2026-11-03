package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Andromedan Automatons — Star Trek #2
 * {2}{W} · Artifact Creature — Android · 3/1
 *
 * When this creature enters, you may search your library for any number of cards named
 * Andromedan Automatons, reveal them, put them into your hand, then shuffle.
 *
 * Same unbounded-search pipeline as Battalion Foot Soldier: gather → chooseAnyNumber → reveal to
 * hand → shuffle, wrapped in `Effects.May` so declining skips the search and the shuffle.
 */
val AndromedanAutomatons = card("Andromedan Automatons") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Android"
    power = 3
    toughness = 1
    oracleText = "When this creature enters, you may search your library for any number of cards " +
        "named Andromedan Automatons, reveal them, put them into your hand, then shuffle."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Effects.Pipeline {
                val searchable = gather(
                    CardSource.FromZone(
                        Zone.LIBRARY,
                        Player.You,
                        GameObjectFilter.Any.named("Andromedan Automatons")
                    ),
                    search = true
                )
                val found = chooseAnyNumber(
                    from = searchable,
                    prompt = "Search your library for any number of cards named Andromedan Automatons",
                )
                toHand(found, revealed = true)
                run(Effects.ShuffleLibrary())
                run(EmitLibrarySearchedEventEffect)
            }
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "2"
        artist = "Uzuri"
        flavorText = "The automatons prepared entire colonies, unaware that a nova had consumed their makers' world."
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc386662-0fe3-432f-85a8-9a06e1274245.jpg"
    }
}
