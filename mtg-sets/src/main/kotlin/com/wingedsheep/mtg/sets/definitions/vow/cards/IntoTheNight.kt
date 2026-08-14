package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Into the Night (Innistrad: Crimson Vow) — {3}{R} Sorcery
 *
 * "It becomes night. Discard any number of cards, then draw that many cards plus one."
 *
 * The day/night flip is [Effects.BecomeNight] (CR 731) — it sets the game's designation to night,
 * doing nothing if it is already night. The loot half is the Brass's Tunnel-Grinder rail:
 * [Patterns.Hand.discardAnyNumber] stores the discarded set under `discarded`, and the draw reads its
 * size as `DynamicAmount.VariableReference("discarded_count")` plus one. Discarding zero cards is legal,
 * so the floor is a one-card draw.
 */
val IntoTheNight: CardDefinition = card("Into the Night") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "It becomes night. Discard any number of cards, then draw that many cards plus one."

    spell {
        effect = Effects.Composite(
            Effects.BecomeNight,
            Patterns.Hand.discardAnyNumber(storeAs = "discarded"),
            Effects.DrawCards(
                DynamicAmount.Add(
                    DynamicAmount.VariableReference("discarded_count"),
                    DynamicAmount.Fixed(1),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "163"
        artist = "Valera Lutfullina"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5a96286-0dda-4761-a1c5-241288c36275.jpg?1783924832"
    }
}
