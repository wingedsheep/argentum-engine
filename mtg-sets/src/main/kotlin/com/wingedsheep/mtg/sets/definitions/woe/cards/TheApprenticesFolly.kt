package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * The Apprentice's Folly
 * {2}{U}{R}
 * Enchantment — Saga
 *
 * Chapters I and II target a nontoken creature you control whose name isn't already represented by
 * one of your tokens, then create the printed nonlegendary Reflection copy with haste. Chapter III
 * sacrifices every Reflection you control, including nontoken Reflections and Reflections created
 * by other effects.
 */
val TheApprenticesFolly = card("The Apprentice's Folly") {
    manaCost = "{2}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I, II — Choose target nontoken creature you control that doesn't have the same name as a " +
        "token you control. Create a token that's a copy of it, except it isn't legendary, is a " +
        "Reflection in addition to its other types, and has haste.\n" +
        "III — Sacrifice all Reflections you control."

    fun copyChapter(chapter: Int) = sagaChapter(chapter) {
        val creature = target(
            "target nontoken creature you control that doesn't have the same name as a token you control",
            TargetCreature(
                filter = TargetFilter(
                    GameObjectFilter.Creature.youControl().nontoken().nameNotSharedWithControlledToken()
                ),
            ),
        )
        effect = Effects.CreateTokenCopyOfTarget(
            target = creature,
            addedKeywords = setOf(Keyword.HASTE),
            removedSupertypes = setOf(Supertype.LEGENDARY),
            addedSubtypes = setOf(Subtype("Reflection")),
        )
    }

    copyChapter(1)
    copyChapter(2)

    sagaChapter(3) {
        effect = Effects.SacrificeAll(GameObjectFilter.Creature.youControl().withSubtype("Reflection"))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "200"
        artist = "Tuan Duong Chu"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0edb58bb-8ff3-4e34-b3d1-d83b5bd8c178.jpg?1783915073"

        ruling(
            "2023-09-01",
            "Except for the listed exceptions, the token copies exactly what was printed on the " +
                "original creature and nothing else."
        )
        ruling("2023-09-01", "If the copied creature has {X} in its mana cost, X is 0.")
        ruling(
            "2023-09-01",
            "Any enters-the-battlefield abilities of the copied creature will trigger when the token " +
                "enters the battlefield. Any as-enters or enters-with abilities will also work."
        )
        ruling(
            "2023-09-01",
            "If something becomes a copy of the token, the copy is also a Reflection in addition to " +
                "its other types, has haste, and isn't legendary."
        )
    }
}
