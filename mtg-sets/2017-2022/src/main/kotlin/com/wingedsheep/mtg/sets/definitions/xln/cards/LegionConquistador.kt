package com.wingedsheep.mtg.sets.definitions.xln.cards

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
 * Legion Conquistador
 * {2}{W}
 * Creature — Vampire Soldier
 * 2/2
 *
 * When this creature enters, you may search your library for any number of cards named
 * Legion Conquistador, reveal them, put them into your hand, then shuffle.
 *
 * Same shape as Gathering Throng: optional name-filtered library search, choose any number,
 * reveal into hand, shuffle.
 */
val LegionConquistador = card("Legion Conquistador") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Vampire Soldier"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, you may search your library for any number of cards named Legion Conquistador, reveal them, put them into your hand, then shuffle."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Effects.Pipeline {
                val searchable = gather(
                    CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Any.named("Legion Conquistador")),
                    search = true
                )
                val found = chooseAnyNumber(
                    from = searchable,
                    prompt = "Search your library for any number of cards named Legion Conquistador",
                )
                toHand(found, revealed = true)
                run(Effects.ShuffleLibrary())
                run(EmitLibrarySearchedEventEffect)
            }
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "20"
        artist = "Sidharth Chaturvedi"
        flavorText = "The Legion of Dusk sailed from Torrezon in search of glory, riches, and the Immortal Sun."
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f30590d7-59c7-4a1a-a8e5-3cc13f69bd41.jpg?1783935799"
    }
}
