package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Trumpeting Gnarr
 * {1}{G}{U}
 * Creature — Beast
 * 3/3
 * Mutate {3}{G/U}{G/U}
 * Whenever this creature mutates, create a 3/3 green Beast creature token.
 */
val TrumpetingGnarr = card("Trumpeting Gnarr") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Beast"
    power = 3
    toughness = 3
    oracleText = "Mutate {3}{G/U}{G/U} (If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it.)\n" +
        "Whenever this creature mutates, create a 3/3 green Beast creature token."

    keywordAbility(KeywordAbility.mutate("{3}{G/U}{G/U}"))

    triggeredAbility {
        trigger = Triggers.self.mutates()
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Beast"),
            imageUri = "https://cards.scryfall.io/normal/front/1/f/1f2c9241-9cba-4c99-85cc-7e74286d1dc1.jpg?1783930944",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "213"
        artist = "Aaron Miller"
        imageUri = "https://cards.scryfall.io/normal/front/0/6/063a95ee-3fda-436f-9ff8-de80cc874dde.jpg?1783931014"
    }
}
