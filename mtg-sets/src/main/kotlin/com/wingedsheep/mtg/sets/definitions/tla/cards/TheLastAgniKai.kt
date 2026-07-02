package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * The Last Agni Kai — {1}{R}
 * Instant
 * Target creature you control fights target creature an opponent controls. If the creature the
 * opponent controls is dealt excess damage this way, add that much {R}.
 * Until end of turn, you don't lose unspent red mana as steps and phases end.
 *
 * Modeling notes:
 *  - A symmetric [Effects.Fight] between "creature you control" (target1) and "creature an opponent
 *    controls" (target2). The fight stores the excess damage (CR 120.4a) it dealt **to target2**
 *    into the pipeline number variable `excess` via `excessDamageVariable`.
 *  - "Add that much {R}" reads that variable with `DynamicAmount.VariableReference("excess")`; an
 *    excess of 0 adds nothing. The composite threads the stored number from the fight into the
 *    AddMana step.
 *  - "Until end of turn, you don't lose unspent red mana as steps and phases end" is the
 *    colour-filtered, turn-scoped [Effects.RetainUnspentMana] (`{R}`) — the one-shot cousin of
 *    Upwelling's `PreventManaPoolEmptying`, sparing only this player's red mana at the turn's
 *    end-of-turn pool emptying.
 */
val TheLastAgniKai = card("The Last Agni Kai") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target creature you control fights target creature an opponent controls. " +
        "If the creature the opponent controls is dealt excess damage this way, add that much {R}.\n" +
        "Until end of turn, you don't lose unspent red mana as steps and phases end."

    spell {
        val yourCreature = target(
            "target creature you control",
            TargetCreature(filter = TargetFilter.CreatureYouControl)
        )
        val theirCreature = target(
            "target creature an opponent controls",
            TargetCreature(filter = TargetFilter.CreatureOpponentControls)
        )
        effect = Effects.Fight(yourCreature, theirCreature, excessDamageVariable = "excess")
            .then(Effects.AddMana(Color.RED, DynamicAmount.VariableReference("excess")))
            .then(Effects.RetainUnspentMana(Color.RED))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "144"
        artist = "Pablo Rivera"
        flavorText = "Zuko's journey began when he refused to duel his father, " +
            "but it would end dueling his sister."
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61eaebc6-7575-48ed-b212-ff8b0c7ae694.jpg?1764120987"
    }
}
