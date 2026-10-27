package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Inga Rune-Eyes — Kaldheim #64
 * {3}{U} · Legendary Creature — Human Wizard · 3/3
 *
 * When Inga Rune-Eyes enters, scry 3.
 * When Inga Rune-Eyes dies, draw three cards if three or more creatures died this turn.
 *
 * The dies ability is not an intervening-if: it always triggers, and the count is checked as it
 * resolves ([Effects.If]). `creaturesDiedThisTurn(Player.Each)` sums every seat's tracker, so
 * Inga herself, creatures that died alongside her, and creatures that died while the ability was
 * on the stack all count, whoever controlled them.
 */
val IngaRuneEyes = card("Inga Rune-Eyes") {
    manaCost = "{3}{U}"
    typeLine = "Legendary Creature — Human Wizard"
    power = 3
    toughness = 3
    oracleText = "When Inga Rune-Eyes enters, scry 3.\n" +
        "When Inga Rune-Eyes dies, draw three cards if three or more creatures died this turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Scry(3)
        description = "When Inga Rune-Eyes enters, scry 3."
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.creaturesDiedThisTurn(Player.Each),
                ComparisonOperator.GTE,
                3,
            ),
            then = Effects.DrawCards(3),
        )
        description = "When Inga Rune-Eyes dies, draw three cards if three or more creatures died this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Bram Sels"
        flavorText = "Every path ever charted by an Omenseeker is etched into her eyes."
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62a6f095-5beb-42a8-af8f-d40eef27150e.jpg?1783928261"
        ruling(
            "2021-02-05",
            "The number of creatures that have died that turn is checked as the last ability resolves. " +
                "Inga Rune-Eyes will count, as will creatures that die at the same time as Inga Rune-Eyes " +
                "and creatures that die while that ability is on the stack but has not yet resolved."
        )
        ruling("2021-02-05", "The last ability will count any creature that died, no matter who controlled it.")
    }
}
