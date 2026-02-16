package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DynamicAmount
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.Player

/**
 * Ixidor's Will
 * {2}{U}
 * Instant
 * Counter target spell unless its controller pays {2} for each Wizard on the battlefield.
 */
val IxidorsWill = card("Ixidor's Will") {
    manaCost = "{2}{U}"
    typeLine = "Instant"
    oracleText = "Counter target spell unless its controller pays {2} for each Wizard on the battlefield."

    spell {
        target = Targets.Spell
        effect = Effects.CounterUnlessDynamicPays(
            DynamicAmount.Multiply(
                DynamicAmount.CountBattlefield(Player.Each, GameObjectFilter.Creature.withSubtype("Wizard")),
                2
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "90"
        artist = "Eric Peterson"
        flavorText = "\"Some dreams should not come to be.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/0/00c29698-0135-41e3-8046-97cbff2ccd24.jpg?1562895063"
    }
}
