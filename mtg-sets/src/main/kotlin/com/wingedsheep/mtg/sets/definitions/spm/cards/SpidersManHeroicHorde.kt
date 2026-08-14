package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.webSlinging
import com.wingedsheep.sdk.model.Rarity

/**
 * Spiders-Man, Heroic Horde — Marvel's Spider-Man #117
 * {1}{G} · Legendary Creature — Spider Hero · 2/3
 *
 * Web-slinging {4}{G}{G}
 * When Spiders-Man enters, if they were cast using web-slinging, you gain 3 life and create two
 * 2/1 green Spider creature tokens with reach.
 *
 * The web-slinging cost ({4}{G}{G}) is deliberately *higher* than the {1}{G} mana cost — a tempo
 * trade that returns a tapped creature to trigger the payoff. Modeled as an intervening-'if' trigger
 * (CR 603.4) gated on the durable "cast using web-slinging" flag via
 * [Conditions.WebSlungCostWasPaid] (CR 702.188), so the ability only goes on the stack on a web-slung
 * cast — mirroring Leonardo, Leader in Blue's "if his sneak cost was paid" ETB.
 */
val SpidersManHeroicHorde = card("Spiders-Man, Heroic Horde") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Spider Hero"
    power = 2
    toughness = 3
    oracleText = "Web-slinging {4}{G}{G} (You may cast this spell for {4}{G}{G} if you also return " +
        "a tapped creature you control to its owner's hand.)\n" +
        "When Spiders-Man enters, if they were cast using web-slinging, you gain 3 life and create " +
        "two 2/1 green Spider creature tokens with reach."

    webSlinging("{4}{G}{G}")

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.WebSlungCostWasPaid
        effect = Effects.GainLife(3) then Effects.CreateToken(
            power = 2,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Spider"),
            keywords = setOf(Keyword.REACH),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/4/a/4a40f6e1-3545-4503-af3e-f0acfb735e3a.jpg?1783905184"
        )
        description = "When Spiders-Man enters, if they were cast using web-slinging, you gain 3 " +
            "life and create two 2/1 green Spider creature tokens with reach."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "117"
        artist = "Lars Grant-West"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/1183262e-1f02-46b3-8cfa-fe30e0016c11.jpg?1783905323"
    }
}
