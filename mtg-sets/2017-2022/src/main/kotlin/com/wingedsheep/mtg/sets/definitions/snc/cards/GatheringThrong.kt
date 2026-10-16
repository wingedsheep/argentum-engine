package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Gathering Throng
 * {2}{W}
 * Creature — Human Citizen
 * 3/1
 * When this creature enters, you may search your library for any number of cards named Gathering
 * Throng, reveal them, put them into your hand, then shuffle.
 *
 * The search is unbounded, so it is the inline library pipeline with `chooseAnyNumber` (as on
 * Grozoth) rather than `Patterns.Library.searchLibrary`, which only offers `ChooseUpTo(count)`.
 * The "you may" gate wraps the whole pipeline, so declining skips the shuffle too.
 */
val GatheringThrong = card("Gathering Throng") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Citizen"
    power = 3
    toughness = 1
    oracleText = "When this creature enters, you may search your library for any number of cards named Gathering Throng, reveal them, put them into your hand, then shuffle."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Effects.Pipeline {
                val searchable = gather(
                    CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Any.named("Gathering Throng"))
                )
                val found = chooseAnyNumber(
                    from = searchable,
                    prompt = "Search your library for any number of cards named Gathering Throng",
                )
                toHand(found, revealed = true)
                run(Effects.ShuffleLibrary())
            }
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "13"
        artist = "Randy Vargas"
        flavorText = "A plus-one is simply not enough."
        imageUri = "https://cards.scryfall.io/normal/front/2/9/290347b9-d3cc-4882-8da6-9d8b8677f276.jpg?1783923158"
    }
}
