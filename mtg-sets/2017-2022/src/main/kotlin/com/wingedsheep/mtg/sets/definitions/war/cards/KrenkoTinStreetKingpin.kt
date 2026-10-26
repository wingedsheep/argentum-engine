package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Krenko, Tin Street Kingpin
 * {2}{R}
 * Legendary Creature — Goblin
 * 1/2
 * Whenever Krenko attacks, put a +1/+1 counter on it, then create a number of 1/1 red Goblin
 * creature tokens equal to Krenko's power.
 *
 * The token count reads Krenko's power after the counter lands. If Krenko has left the battlefield
 * by resolution, the counter is skipped and the count falls back to its last-known power (ruling) —
 * `EntityProperty(Self, Power)` reads the departed object's battlefield-exit snapshot. The tokens
 * aren't attacking.
 */
val KrenkoTinStreetKingpin = card("Krenko, Tin Street Kingpin") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Goblin"
    power = 1
    toughness = 2
    oracleText = "Whenever Krenko attacks, put a +1/+1 counter on it, then create a number of 1/1 red Goblin " +
        "creature tokens equal to Krenko's power."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Effects.CreateToken(
                count = DynamicAmounts.sourcePower(),
                power = 1,
                toughness = 1,
                colors = setOf(Color.RED),
                creatureTypes = setOf("Goblin"),
                imageUri = "https://cards.scryfall.io/normal/front/c/0/c0861b13-6cf8-4078-9178-e223a4041a8b.jpg?1783933350",
            )
        description = "Whenever Krenko attacks, put a +1/+1 counter on it, then create a number of 1/1 red " +
            "Goblin creature tokens equal to Krenko's power."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "137"
        artist = "Mark Behm"
        flavorText = "\"After the people flee, but before the enemy arrives—that's grabbin' time.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37ed04d3-cfa1-4778-aea6-b4c2c29e6e0a.jpg?1783933423"
        ruling(
            "2019-05-03",
            "If Krenko leaves the battlefield after its ability has triggered but before it resolves, you don't put " +
                "a +1/+1 counter on it, but you do use its power as it last existed before it left the battlefield " +
                "to determine how many Goblin tokens to create."
        )
        ruling(
            "2019-05-03",
            "The tokens created by Krenko's triggered ability aren't attacking. Because all attackers are chosen at " +
                "once, a token created this way can't attack, even if it gains haste."
        )
    }
}
