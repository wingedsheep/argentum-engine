package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanAttackDespiteDefender
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Demon Wall — Final Fantasy #97
 * {1}{B} · Artifact Creature — Demon Wall · 3/3
 *
 * Defender
 * Menace
 * As long as this creature has a counter on it, it can attack as though it didn't have defender.
 * {5}{B}: Put two +1/+1 counters on this creature.
 */
val DemonWall = card("Demon Wall") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Artifact Creature — Demon Wall"
    power = 3
    toughness = 3
    oracleText = "Defender\nMenace\nAs long as this creature has a counter on it, it can attack as though it didn't have defender.\n{5}{B}: Put two +1/+1 counters on this creature."

    keywords(Keyword.DEFENDER, Keyword.MENACE)

    staticAbility {
        ability = CanAttackDespiteDefender(
            condition = Conditions.SourceHasCounter(CounterTypeFilter.Any)
        )
    }

    activatedAbility {
        cost = Costs.Mana("{5}{B}")
        effect = AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "Anton Solovianchyk"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13abd96c-d1af-43d0-b3a4-ac3db20e3b51.jpg?1748706126"
    }
}
