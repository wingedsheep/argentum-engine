package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * The Shattered States Era // Nameless Conqueror
 * {4}{R} — Enchantment — Saga // Enchantment Creature — Human Samurai 3/3
 *
 * Front — The Shattered States Era:
 *   I — Gain control of target creature until end of turn. Untap it. It gains haste until end of turn.
 *   II — Creatures you control get +1/+0 until end of turn.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 *
 * Back — Nameless Conqueror:
 *   Trample, haste
 */
private val NamelessConqueror = card("Nameless Conqueror") {
    manaCost = ""
    colorIndicator = "R"
    colorIdentity = "R"
    typeLine = "Enchantment Creature — Human Samurai"
    oracleText = "Trample, haste"
    power = 3
    toughness = 3

    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "162"
        artist = "David Gaillet"
        flavorText = "\"In the wake of the Kami War, Kamigawa descended into a state of constant warfare. " +
            "Even with Kyodai's wisdom guiding them, it took the first Imperials decades of work and " +
            "bloodshed to subdue the clans and bring peace to the land.\"\n—History of the Empire"
        imageUri = "https://cards.scryfall.io/normal/back/5/1/517392f6-d3fd-4c59-8317-2a3595ab7562.jpg?1783923865"
    }
}

private val TheShatteredStatesEraFront = card("The Shattered States Era") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I — Gain control of target creature until end of turn. Untap it. It gains haste until end of turn.\n" +
        "II — Creatures you control get +1/+0 until end of turn.\n" +
        "III — Exile this Saga, then return it to the battlefield transformed under your control."

    sagaChapter(1) {
        val creature = target(TargetFilter.Creature)
        effect = Effects.GainControl(creature, Duration.EndOfTurn) then
            Effects.Untap(creature) then
            Effects.GrantKeyword(Keyword.HASTE, creature)
    }

    sagaChapter(2) {
        effect = Patterns.Group.modifyStatsForAll(
            1, 0,
            GroupFilter(GameObjectFilter.Creature.youControl())
        )
    }

    sagaChapter(3) {
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "162"
        artist = "David Gaillet"
        imageUri = "https://cards.scryfall.io/normal/front/5/1/517392f6-d3fd-4c59-8317-2a3595ab7562.jpg?1783923865"
    }
}

val TheShatteredStatesEra: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = TheShatteredStatesEraFront,
    backFace = NamelessConqueror,
)
