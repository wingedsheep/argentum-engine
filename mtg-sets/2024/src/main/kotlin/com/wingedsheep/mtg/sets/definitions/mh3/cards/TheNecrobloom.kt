package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GraveyardCardsHaveDredge
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * The Necrobloom
 * {1}{W}{B}{G}
 * Legendary Creature — Plant
 * 2/7
 *
 * Landfall — Whenever a land you control enters, create a 0/1 green Plant creature token. If you
 * control seven or more lands with different names, create a 2/2 black Zombie creature token instead.
 * Land cards in your graveyard have dredge 2.
 *
 * The seven-names check is made as the trigger resolves, so every land that entered alongside the
 * triggering one counts (Scapeshift makes seven Zombies).
 */
val TheNecrobloom = card("The Necrobloom") {
    manaCost = "{1}{W}{B}{G}"
    typeLine = "Legendary Creature — Plant"
    power = 2
    toughness = 7
    oracleText = "Landfall — Whenever a land you control enters, create a 0/1 green Plant creature " +
        "token. If you control seven or more lands with different names, create a 2/2 black Zombie " +
        "creature token instead.\n" +
        "Land cards in your graveyard have dredge 2. (You may return a land card from your graveyard " +
        "to your hand and mill two cards instead of drawing a card.)"

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.differentlyNamedLandsYouControl(),
                ComparisonOperator.GTE,
                7
            ),
            then = Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Zombie"),
                imageUri = "https://cards.scryfall.io/normal/front/4/c/4c923eed-3d09-4b38-a884-513700aebca3.jpg?1783911106"
            ),
            otherwise = Effects.CreateToken(
                power = 0,
                toughness = 1,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Plant"),
                imageUri = "https://cards.scryfall.io/normal/front/9/0/909f85b2-0d7b-42ac-a0b7-402e20cedab4.jpg?1783911105"
            )
        )
        description = "Landfall — Whenever a land you control enters, create a 0/1 green Plant " +
            "creature token. If you control seven or more lands with different names, create a 2/2 " +
            "black Zombie creature token instead."
    }

    staticAbility {
        ability = GraveyardCardsHaveDredge(GameObjectFilter.Land, amount = 2)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "194"
        artist = "Igor Krstic"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8fcf68cf-0dac-4b29-90d5-c18c685182e6.jpg?1783911249"
        ruling(
            "2024-06-07",
            "If you control multiple lands with the same name, only one of those lands will count " +
                "toward the seven or more required to create a Zombie."
        )
        ruling(
            "2024-06-07",
            "If multiple lands enter the battlefield under your control simultaneously, all of " +
                "those lands are counted. For example, if you sacrifice seven lands while resolving " +
                "Scapeshift and search your library for seven lands with different names, you'll " +
                "create seven Zombie tokens."
        )
        ruling(
            "2024-06-07",
            "Dredge can replace any card draw, not just the one during your draw step. One card " +
                "draw can't be replaced by multiple dredge abilities."
        )
    }
}
