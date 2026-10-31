package com.wingedsheep.mtg.sets.definitions.m20.cards

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
 * Battalion Foot Soldier — Core Set 2020 #7
 * {2}{W} · Creature — Human Soldier · 2/2
 *
 * When this creature enters, you may search your library for any number of cards named
 * Battalion Foot Soldier, reveal them, put them into your hand, then shuffle.
 *
 * An unbounded search, so it is Iname, Death Aspect's inline gather → chooseAnyNumber → move
 * pipeline rather than `Patterns.Library.searchLibrary` (which only offers "up to N"). The
 * `Effects.May` gate wraps the whole search, so declining skips the shuffle too; choosing zero
 * cards is still a legal "found nothing" search.
 */
val BattalionFootSoldier = card("Battalion Foot Soldier") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, you may search your library for any number of cards " +
        "named Battalion Foot Soldier, reveal them, put them into your hand, then shuffle."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Effects.Pipeline {
                val searchable = gather(
                    CardSource.FromZone(
                        Zone.LIBRARY,
                        Player.You,
                        GameObjectFilter.Any.named("Battalion Foot Soldier")
                    ),
                    search = true
                )
                val found = chooseAnyNumber(
                    from = searchable,
                    prompt = "Search your library for any number of cards named Battalion Foot Soldier",
                )
                toHand(found, revealed = true)
                run(Effects.ShuffleLibrary())
                run(EmitLibrarySearchedEventEffect)
            }
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "7"
        artist = "Cristi Balanescu"
        flavorText = "\"With me! Break this line and the rest will fall!\""
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1db6393a-88b0-4973-9832-1482f69917ff.jpg?1783933033"
    }
}
