package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect

/**
 * Goblinslide
 * {2}{R}
 * Enchantment
 * Whenever you cast a noncreature spell, you may pay {1}.
 * If you do, create a 1/1 red Goblin creature token with haste.
 */
val Goblinslide = card("Goblinslide") {
    manaCost = "{2}{R}"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast a noncreature spell, you may pay {1}. If you do, create a 1/1 red Goblin creature token with haste."

    triggeredAbility {
        trigger = Triggers.YouCastNoncreature
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{1}"),
            effect = CreateTokenEffect(
                power = 1,
                toughness = 1,
                colors = setOf(Color.RED),
                creatureTypes = setOf("Goblin"),
                keywords = setOf(Keyword.HASTE),
                imageUri = "https://cards.scryfall.io/normal/front/e/d/ed418a8b-f158-492d-a323-6265b3175292.jpg?1562640121"
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "109"
        artist = "Kev Walker"
        flavorText = "Goblins, like snowflakes, are only dangerous in numbers."
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9d80e96-3956-4408-84fb-5f94a364eb41.jpg?1562791695"
    }
}
