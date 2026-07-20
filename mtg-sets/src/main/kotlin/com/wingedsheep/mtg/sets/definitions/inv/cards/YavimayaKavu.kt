package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Yavimaya Kavu
 * {2}{R}{G}
 * Creature — Kavu
 * Power and toughness are each defined dynamically (printed as * / *).
 * Yavimaya Kavu's power is equal to the number of red creatures on the battlefield.
 * Yavimaya Kavu's toughness is equal to the number of green creatures on the battlefield.
 *
 * Both counts span all players' battlefields (Player.Each) and include Yavimaya Kavu
 * itself, which is both red and green.
 */
val YavimayaKavu = card("Yavimaya Kavu") {
    manaCost = "{2}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Kavu"
    oracleText = "Yavimaya Kavu's power is equal to the number of red creatures on the battlefield.\n" +
        "Yavimaya Kavu's toughness is equal to the number of green creatures on the battlefield."

    dynamicPower(
        DynamicAmount.AggregateBattlefield(
            player = Player.Each,
            filter = GameObjectFilter.Creature.withColor(Color.RED)
        )
    )

    dynamicToughness(
        DynamicAmount.AggregateBattlefield(
            player = Player.Each,
            filter = GameObjectFilter.Creature.withColor(Color.GREEN)
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "291"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/1872f104-7cf1-41e3-b1b4-ca75c678e08b.jpg?1562899872"
    }
}
