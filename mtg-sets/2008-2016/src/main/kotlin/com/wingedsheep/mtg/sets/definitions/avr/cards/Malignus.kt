package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.divRoundedUp
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DamageCantBePrevented
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Malignus {3}{R}{R}
 * Creature — Elemental Spirit
 * * / *
 *
 * Malignus's power and toughness are each equal to half the highest life total among your
 * opponents, rounded up.
 * Damage that would be dealt by this creature can't be prevented.
 *
 * - The P/T is a characteristic-defining ability ([dynamicStats]) that applies in every zone:
 *   `GreatestAmongPlayers(LifeTotal, EachOpponent)` divided by 2, rounded up — the same
 *   "highest life total among your opponents" read as Linvala, the Preserver.
 * - The damage clause is Excruciator's [DamageCantBePrevented] scoped to its own source, so
 *   protection from red doesn't stop its damage (ruling 2012-05-01).
 */
val Malignus = card("Malignus") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental Spirit"
    oracleText = "Malignus's power and toughness are each equal to half the highest life total " +
        "among your opponents, rounded up.\n" +
        "Damage that would be dealt by this creature can't be prevented."

    val highestOpponentLife =
        DynamicAmounts.greatestAmongPlayers(DynamicAmounts.lifeTotal(Player.You), Player.EachOpponent)
    dynamicStats(highestOpponentLife divRoundedUp 2)

    replacementEffect(
        DamageCantBePrevented(appliesTo = EventPattern.DamageEvent(source = GameObjectFilter.Any.sourceItself()))
    )

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "148"
        artist = "Jung Park"
        flavorText = "It descended on Estwald like a sudden storm. Moments later, only ashes and agony remained."
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a6a7000-4a1d-4cd4-a85e-4b7b20d8e543.jpg?1783940680"
        ruling(
            "2012-05-01",
            "If Malignus becomes blocked by a creature with protection from red, the damage Malignus " +
                "deals to that creature won't be prevented."
        )
    }
}
