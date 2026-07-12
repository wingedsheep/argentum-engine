package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Flourishing Hunter
 * {4}{G}{G}
 * Creature — Wolf Spirit
 * 6/6
 *
 * When this creature enters, you gain life equal to the greatest toughness among other creatures
 * you control.
 *
 * ETB gain life keyed to a MAX-toughness [DynamicAmount.AggregateBattlefield] over creatures you
 * control with `excludeSelf = true` — the "other creatures" wording drops Flourishing Hunter's own
 * 6 toughness from the aggregate (Valley Rotcaller idiom).
 */
val FlourishingHunter = card("Flourishing Hunter") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wolf Spirit"
    power = 6
    toughness = 6
    oracleText = "When this creature enters, you gain life equal to the greatest toughness among " +
        "other creatures you control."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.GainLife(
            DynamicAmount.AggregateBattlefield(
                player = Player.You,
                filter = GameObjectFilter.Creature,
                aggregation = Aggregation.MAX,
                property = CardNumericProperty.TOUGHNESS,
                excludeSelf = true
            )
        )
        description = "When this creature enters, you gain life equal to the greatest toughness " +
            "among other creatures you control."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "199"
        artist = "Ilse Gort"
        flavorText = "\"I would welcome her into my pack with honor.\"\n—Arlinn Kord"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2abc54df-773d-4f46-b241-e1f42af8ab95.jpg?1782703051"
    }
}
