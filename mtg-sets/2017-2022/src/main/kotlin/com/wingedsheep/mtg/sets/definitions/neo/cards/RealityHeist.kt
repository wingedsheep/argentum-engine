package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Reality Heist — Kamigawa: Neon Dynasty #75
 * {5}{U}{U} · Instant
 *
 * Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)
 * Look at the top seven cards of your library. You may reveal up to two artifact cards from among
 * them and put them into your hand. Put the rest on the bottom of your library in a random order.
 *
 * Pure composition: [KeywordAbility.Affinity] for artifacts (as on Lens Flare) plus the
 * look-at-top / `chooseUpToSplit` dig used by Pictures of Spider-Man, filtered to artifact cards.
 */
val RealityHeist = card("Reality Heist") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)\n" +
        "Look at the top seven cards of your library. You may reveal up to two artifact cards from " +
        "among them and put them into your hand. Put the rest on the bottom of your library in a " +
        "random order."

    keywordAbility(KeywordAbility.Affinity(CardType.ARTIFACT))

    spell {
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(7))
            val (kept, rest) = chooseUpToSplit(
                2,
                from = looked,
                filter = GameObjectFilter.Artifact,
                prompt = "You may reveal up to two artifact cards to put into your hand.",
                showAllCards = true
            )
            toHand(kept, revealed = true)
            toLibraryBottom(rest, order = CardOrder.Random)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "75"
        artist = "Mila Pesic"
        flavorText = "Kaito grabbed the chip, hoping it held answers."
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5bc954d7-322a-45b8-9e25-6e9a97fbde61.jpg?1783923895"
    }
}
