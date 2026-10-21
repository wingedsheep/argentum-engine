package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tolarian Contempt — March of the Machine: The Aftermath #8
 * {3}{U}{U} · Enchantment
 *
 * "For each opponent, choose up to one target creature they control with a rejection counter on
 * it" is the corpus's one-per-opponent distribution (The Balrog of Moria): up to one target per
 * opponent, no two sharing a controller. Each target's owner then picks top or bottom of their own
 * library.
 */
val TolarianContempt = card("Tolarian Contempt") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, put a rejection counter on each creature your opponents control.\n" +
        "At the beginning of your end step, for each opponent, choose up to one target creature they " +
        "control with a rejection counter on it. That creature's owner puts it on their choice of the " +
        "top or bottom of their library."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.opponentControls()),
            Effects.AddCounters(CounterType.REJECTION, 1, EffectTarget.IterationEntity)
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        targets(
            TargetFilter(GameObjectFilter.Creature.opponentControls().withCounter(CounterType.REJECTION)),
            optional = true,
            dynamicMaxCount = DynamicAmounts.playerCount(Player.EachOpponent),
            differentControllers = true,
        )
        effect = Effects.ForEachTarget(Effects.PutOnTopOrBottomOfLibrary(EffectTarget.ContextTarget(0)))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "8"
        artist = "Tuan Duong Chu"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f72a9deb-9329-4930-a906-136c26490d52.jpg?1783916526"
    }
}
