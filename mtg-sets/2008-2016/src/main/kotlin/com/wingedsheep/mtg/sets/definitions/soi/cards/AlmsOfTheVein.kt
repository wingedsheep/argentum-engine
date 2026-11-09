package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Rarity

/**
 * Alms of the Vein (Shadows over Innistrad #98)
 * {2}{B}
 * Sorcery
 *
 * Target opponent loses 3 life and you gain 3 life.
 * Madness {B}
 */
val AlmsOfTheVein = card("Alms of the Vein") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent loses 3 life and you gain 3 life.\n" +
        "Madness {B} (If you discard this card, discard it into exile. When you do, cast it for its " +
        "madness cost or put it into your graveyard.)"

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(3, opponent) then Effects.GainLife(3)
    }

    madness("{B}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "98"
        artist = "David Gaillet"
        imageUri = "https://cards.scryfall.io/normal/front/7/9/79b80948-a3cd-4962-8fce-d58f2db7e68e.jpg"
    }
}
