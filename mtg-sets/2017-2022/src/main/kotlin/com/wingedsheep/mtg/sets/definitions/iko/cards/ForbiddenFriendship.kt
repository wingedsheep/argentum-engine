package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Forbidden Friendship
 * {1}{R}
 * Sorcery
 * Create a 1/1 red Dinosaur creature token with haste and a 1/1 white Human Soldier creature token.
 */
val ForbiddenFriendship = card("Forbidden Friendship") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Create a 1/1 red Dinosaur creature token with haste and a 1/1 white Human Soldier creature token."

    spell {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Dinosaur"),
            keywords = setOf(Keyword.HASTE),
            imageUri = "https://cards.scryfall.io/normal/front/f/9/f918b740-1984-4090-8886-9e290a698b95.jpg?1783930944",
        ) then Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human", "Soldier"),
            imageUri = "https://cards.scryfall.io/normal/front/e/4/e406949f-c629-48fa-9fb0-ba6191054d35.jpg?1783930945",
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "119"
        artist = "Jakub Kasper"
        flavorText = "The two looked at each other, and neither fled. \"I see,\" the soldier murmured. \"We're both just fighting for survival out here, aren't we?\""
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e634baa3-2cdd-412a-9407-c347fe46f9b8.jpg?1783931050"
    }
}
