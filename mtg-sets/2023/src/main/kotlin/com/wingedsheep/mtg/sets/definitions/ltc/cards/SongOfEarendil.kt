package com.wingedsheep.mtg.sets.definitions.ltc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Song of Eärendil
 * {3}{G}{U}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Scry 2, then draw two cards.
 * II — Create a Treasure token and a 2/2 blue Bird creature token with flying.
 * III — Put a flying counter on each creature you control without flying.
 *
 * Chapter II creates both tokens in one creation event. Chapter III reads projected keywords,
 * so a creature that has flying only from a continuous effect is skipped.
 */
private const val BIRD_TOKEN_IMAGE =
    "https://cards.scryfall.io/normal/front/c/8/c813d283-c996-4f85-afaa-7702414a1197.jpg?1783916058"

val SongOfEarendil = card("Song of Eärendil") {
    manaCost = "{3}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Scry 2, then draw two cards.\n" +
        "II — Create a Treasure token and a 2/2 blue Bird creature token with flying.\n" +
        "III — Put a flying counter on each creature you control without flying."

    sagaChapter(1) {
        effect = Patterns.Library.scry(2) then Effects.DrawCards(2)
    }

    sagaChapter(2) {
        effect = Effects.CreateTokensTogether(
            Effects.CreateTreasure(),
            Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.BLUE),
                creatureTypes = setOf("Bird"),
                keywords = setOf(Keyword.FLYING),
                imageUri = BIRD_TOKEN_IMAGE,
            ),
        )
    }

    sagaChapter(3) {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl().withoutKeyword(Keyword.FLYING)),
            Effects.AddCounters(CounterType.FLYING, 1, EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "69"
        artist = "Torgeir Fjereide"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/325bb6b7-6d19-48b5-82dd-2759d4fc3144.jpg?1783916015"
    }
}
