package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Oona's Gatewarden
 * {U/B}
 * Creature — Faerie Soldier
 * 2 / 1
 *
 * Defender, flying
 * Wither (This deals damage to creatures in the form of -1/-1 counters.)
 *
 * - Keyword-only creature. [Keyword.WITHER] is engine-live — damage this deals to a creature
 *   becomes -1/-1 counters — so the reminder text needs no separate replacement effect.
 */
val OonasGatewarden = card("Oona's Gatewarden") {
    manaCost = "{U/B}"
    typeLine = "Creature — Faerie Soldier"
    power = 2
    toughness = 1
    oracleText = "Defender, flying\n" +
        "Wither (This deals damage to creatures in the form of -1/-1 counters.)"

    keywords(Keyword.DEFENDER, Keyword.FLYING, Keyword.WITHER)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "173"
        artist = "Mike Dringenberg"
        flavorText = "\"So now you've seen Glen Elendra. Take a good look. It will be the last thing you'll ever see.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/7/972bb6e4-300c-49f5-8305-459a3ba67baa.jpg?1783942729"
    }
}
