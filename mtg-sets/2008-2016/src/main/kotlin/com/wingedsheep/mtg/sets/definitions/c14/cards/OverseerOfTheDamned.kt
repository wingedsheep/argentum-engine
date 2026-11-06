package com.wingedsheep.mtg.sets.definitions.c14.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Overseer of the Damned
 * {5}{B}{B}
 * Creature — Demon
 * 5/5
 * Flying
 * When this creature enters, you may destroy target creature.
 * Whenever a nontoken creature an opponent controls dies, create a tapped 2/2 black Zombie creature
 * token.
 *
 * The enter trigger is an optional targeted [Effects.Destroy]. The dies trigger watches nontoken
 * creatures an opponent controlled (last-known controller) and creates the Zombie tapped.
 */
val OverseerOfTheDamned = card("Overseer of the Damned") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    power = 5
    toughness = 5
    oracleText = "Flying\n" +
        "When this creature enters, you may destroy target creature.\n" +
        "Whenever a nontoken creature an opponent controls dies, create a tapped 2/2 black Zombie creature token."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        val creature = target(TargetFilter.Creature)
        effect = Effects.Destroy(creature)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.nontoken().opponentControls()).dies()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            tapped = true,
            imageUri = "https://cards.scryfall.io/normal/front/0/3/0397aa00-6d82-47d9-bc30-072b722c8b71.jpg?1783938797",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "rk post"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d1a73408-27b3-45d8-9153-ca964d99fe80.jpg?1783938868"
    }
}
