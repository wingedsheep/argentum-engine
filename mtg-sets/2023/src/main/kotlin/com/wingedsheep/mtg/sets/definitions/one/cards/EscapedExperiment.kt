package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Escaped Experiment
 * {1}{U}
 * Artifact Creature — Phyrexian Beast
 * 2/1
 *
 * Whenever this creature attacks, target creature an opponent controls gets -X/-0 until end of
 * turn, where X is the number of artifacts you control.
 *
 * X is counted once, as the ability resolves (the Silvergill Douser idiom). The Experiment is itself
 * an artifact, so X is at least 1 while it is still on the battlefield.
 */
val EscapedExperiment = card("Escaped Experiment") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Phyrexian Beast"
    power = 2
    toughness = 1
    oracleText = "Whenever this creature attacks, target creature an opponent controls gets -X/-0 until end of turn, " +
        "where X is the number of artifacts you control."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.ModifyStats(
            power = -DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count(),
            toughness = DynamicAmounts.fixed(0),
            target = creature
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "48"
        artist = "Billy Christian"
        flavorText = "One morning, the test subject decided to perform trials on the scientists."
        imageUri = "https://cards.scryfall.io/normal/front/6/1/611497d2-83df-4c69-bbeb-6a807c686bc4.jpg?1783918066"
    }
}
