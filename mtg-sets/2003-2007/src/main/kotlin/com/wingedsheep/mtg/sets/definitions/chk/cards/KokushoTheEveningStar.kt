package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Kokusho, the Evening Star
 * {4}{B}{B}
 * Legendary Creature — Dragon Spirit
 * 5/5
 * Flying
 * When Kokusho dies, each opponent loses 5 life. You gain life equal to the life lost this way.
 *
 * Exsanguinate's drain on a dies trigger: [Effects.DrainLife] gains the life *actually* lost, so
 * an opponent who can't lose life (or no opponent at all) yields nothing.
 */
val KokushoTheEveningStar = card("Kokusho, the Evening Star") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Dragon Spirit"
    power = 5
    toughness = 5
    oracleText = "Flying\n" +
        "When Kokusho dies, each opponent loses 5 life. You gain life equal to the life lost this way."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.DrainLife(5)
        description = "When Kokusho dies, each opponent loses 5 life. You gain life equal to the life lost this way."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "122"
        artist = "Tsutomu Kawade"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63dc0698-e92f-4134-80e4-5cc37b80e37c.jpg?1783944312"
    }
}
