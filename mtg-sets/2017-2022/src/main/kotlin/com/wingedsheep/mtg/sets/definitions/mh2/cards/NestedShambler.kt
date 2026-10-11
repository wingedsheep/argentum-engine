package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Nested Shambler — Modern Horizons 2 #95
 * {B} · Creature — Zombie · 1/1
 *
 * When this creature dies, create X tapped 1/1 green Squirrel creature tokens, where X is this
 * creature's power.
 *
 * X is the source's power read with last-known information: `DynamicAmounts.sourcePower()` falls
 * back to the battlefield-exit snapshot once the creature has left (CR 608.2h), so a pumped
 * Shambler makes as many Squirrels as its power when it died.
 */
val NestedShambler = card("Nested Shambler") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 1
    toughness = 1
    oracleText = "When this creature dies, create X tapped 1/1 green Squirrel creature tokens, " +
        "where X is this creature's power."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            count = DynamicAmounts.sourcePower(),
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Squirrel"),
            tapped = true,
            imageUri = "https://cards.scryfall.io/normal/front/9/7/977ddd05-1aae-46fc-95ce-866710d1c5c6.jpg?1783926589"
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "95"
        artist = "Nicholas Gregory"
        flavorText = "Home is where the heart was."
        imageUri = "https://cards.scryfall.io/normal/front/9/8/9851f290-f502-49f8-9b48-67f7966d4e34.jpg?1783926858"
        ruling(
            "2021-06-18",
            "X is Nested Shambler's power the last time it was on the battlefield, not its power in " +
                "the graveyard. If its power was 0 or less when it died, you won't create any tokens."
        )
    }
}
