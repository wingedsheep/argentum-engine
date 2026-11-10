package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Seed Guardian — Oath of the Gatewatch #143 (canonical printing)
 * {2}{G}{G} · Creature — Elemental · 3/4
 *
 * Reach
 * When this creature dies, create an X/X green Elemental creature token, where X is the number of
 * creature cards in your graveyard.
 *
 * X is counted as the trigger resolves, so Seed Guardian counts itself while it's still in the
 * graveyard. `CreateDynamicToken` evaluates the amount once at creation and stamps it as the
 * token's base P/T — a set X/X, not a continuously-updating * / *.
 */
val SeedGuardian = card("Seed Guardian") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    power = 3
    toughness = 4
    oracleText = "Reach\nWhen this creature dies, create an X/X green Elemental creature token, where X " +
        "is the number of creature cards in your graveyard."

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.dies()
        val creatureCards = DynamicAmounts.creatureCardsInYourGraveyard()
        effect = Effects.CreateDynamicToken(
            dynamicPower = creatureCards,
            dynamicToughness = creatureCards,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Elemental"),
            imageUri = "https://cards.scryfall.io/normal/front/d/b/db67bc06-b6c9-49a0-beef-4d35842497cb.jpg?1783937859",
        )
        description = "When this creature dies, create an X/X green Elemental creature token, where X is " +
            "the number of creature cards in your graveyard."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "Vincent Proce"
        flavorText = "It is a reminder that spring will always follow winter."
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba4736b3-d6ce-4a16-bcb7-67780caeb78c.jpg?1783937899"
        ruling("2016-01-22", "Use the number of creature cards in your graveyard as the ability resolves to determine the value of X. Seed Guardian will count toward this number as long as it's still in your graveyard at that time.")
        ruling("2016-01-22", "Once the Elemental is created, the number of creature cards in your graveyard changing won't cause the Elemental's power or toughness to change.")
    }
}
