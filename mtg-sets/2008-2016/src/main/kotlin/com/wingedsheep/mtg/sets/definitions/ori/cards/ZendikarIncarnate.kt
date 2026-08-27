package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Zendikar Incarnate
 * {2}{R}{G}
 * Creature — Elemental
 * ★/4
 * Zendikar Incarnate's power is equal to the number of lands you control.
 */
val ZendikarIncarnate = card("Zendikar Incarnate") {
    manaCost = "{2}{R}{G}"
    colorIdentity = "GR"
    typeLine = "Creature — Elemental"
    toughness = 4
    oracleText = "Zendikar Incarnate's power is equal to the number of lands you control."

    dynamicPower(
        DynamicAmount.AggregateBattlefield(
            player = Player.You,
            filter = GameObjectFilter.Land
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "219"
        artist = "Lucas Graciano"
        flavorText = "\"Her people angered Zendikar, and they faced the land's wrath. That is why Nissa is the last of the animists.\"\n—Numa, Joraga chieftain"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb12b1d8-c53e-4d48-89e5-2168ff34a853.jpg?1783938312"

        ruling("2015-06-22", "The ability defining Zendikar Incarnate's power works in all zones, not just the battlefield. Zendikar Incarnate's power changes as the number of lands you control does.")
    }
}
