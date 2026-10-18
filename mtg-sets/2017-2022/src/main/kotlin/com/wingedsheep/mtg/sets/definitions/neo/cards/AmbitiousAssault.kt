package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate

/**
 * Ambitious Assault — Kamigawa: Neon Dynasty #133 (canonical printing)
 * {2}{R} · Instant
 *
 * Creatures you control get +2/+0 until end of turn. If you control a modified creature, draw a
 * card.
 *
 * The modified check happens on resolution, after the pump. "Modified" is
 * [StatePredicate.IsModified] (CR 700.9), which only counts Auras you control.
 */
val AmbitiousAssault = card("Ambitious Assault") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Creatures you control get +2/+0 until end of turn. If you control a modified creature, " +
        "draw a card. (Equipment, Auras you control, and counters are modifications.)"

    spell {
        effect = Patterns.Group.modifyStatsForAll(2, 0, Filters.Group.creaturesYouControl) then
            Effects.If(
                condition = Conditions.YouControl(
                    GameObjectFilter.Creature.withStatePredicate(StatePredicate.IsModified)
                ),
                then = Effects.DrawCards(1),
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "133"
        artist = "Wayne Reynolds"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d00d618d-abad-475b-b938-e111282ea1c3.jpg?1783923871"
    }
}
