package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Malamet Battle Glyph
 * {G}
 * Sorcery
 *
 * Choose target creature you control and target creature you don't control. If the creature you
 * control entered this turn, put a +1/+1 counter on it. Then those creatures fight each other.
 *
 * Target 0 is the creature you control (also the fight's first combatant); target 1 the creature
 * you don't control. The conditional counter is gated on target 0's entered-this-turn status via
 * [Conditions.TargetMatchesFilter], then [Effects.Fight] resolves the fight. "A creature you don't
 * control" is modeled as [GameObjectFilter.Creature.opponentControls] — equivalent, since every
 * creature not controlled by you is controlled by an opponent.
 */
val MalametBattleGlyph = card("Malamet Battle Glyph") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Choose target creature you control and target creature you don't control. If the " +
        "creature you control entered this turn, put a +1/+1 counter on it. Then those creatures fight each other."

    spell {
        val mine = target(
            "target creature you control",
            TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.youControl()))
        )
        val theirs = target(
            "target creature you don't control",
            TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.opponentControls()))
        )
        effect = ConditionalEffect(
            condition = Conditions.TargetMatchesFilter(
                GameObjectFilter.Any.enteredThisTurn(),
                targetIndex = 0
            ),
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0))
        ).then(Effects.Fight(mine, theirs))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "198"
        artist = "Andrew Mar"
        flavorText = "\"Let's not give the surface-dwellers time to learn what tactics work best here.\"\n—Daram, Malamet marshal"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/2259f959-ca97-4df9-8d50-0532090fb967.jpg?1782694450"
    }
}
