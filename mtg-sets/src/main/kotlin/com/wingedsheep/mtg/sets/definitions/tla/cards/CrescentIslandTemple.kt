package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Crescent Island Temple
 * {3}{R}
 * Legendary Enchantment — Shrine
 * When Crescent Island Temple enters, for each Shrine you control, create a 1/1 red Monk
 * creature token with prowess. (Whenever you cast a noncreature spell, it gets +1/+1 until
 * end of turn.)
 * Whenever another Shrine you control enters, create a 1/1 red Monk creature token with prowess.
 *
 * Parallels the Shrine cycle (cf. Northern Air Temple, The Spirit Oasis): the ETB makes one Monk
 * per Shrine you control (this permanent counts itself, as it is already on the battlefield when
 * the trigger resolves), and a second trigger makes one Monk on every *other* Shrine you control
 * entering ([TriggerBinding.OTHER] so Crescent Island Temple's own entry doesn't fire it).
 */
val CrescentIslandTemple = card("Crescent Island Temple") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Enchantment — Shrine"
    oracleText = "When Crescent Island Temple enters, for each Shrine you control, create a 1/1 red " +
        "Monk creature token with prowess. (Whenever you cast a noncreature spell, it gets +1/+1 " +
        "until end of turn.)\n" +
        "Whenever another Shrine you control enters, create a 1/1 red Monk creature token with prowess."

    // The number of Shrines you control (this permanent already counts itself on its own ETB).
    val shrinesYouControl = DynamicAmounts
        .battlefield(Player.You, GameObjectFilter.Any.withSubtype("Shrine"))
        .count()

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            count = shrinesYouControl,
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Monk"),
            keywords = setOf(Keyword.PROWESS),
        )
    }

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Any.withSubtype("Shrine").youControl(),
            binding = TriggerBinding.OTHER,
        )
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Monk"),
            keywords = setOf(Keyword.PROWESS),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "129"
        artist = "Luc Courtois"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04c85150-269e-4158-a1b9-bd11bc6b7d79.jpg?1764120888"
    }
}
