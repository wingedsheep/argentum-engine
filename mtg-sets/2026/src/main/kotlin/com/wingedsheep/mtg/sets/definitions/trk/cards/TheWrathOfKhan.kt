package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerTiming
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Wrath of Khan
 * {2}{R}
 * Enchantment — Saga
 *
 * Chapter I deals 4 damage, then schedules the threaten for the next beginning of combat this
 * turn. The delayed trigger bakes "that creature" to the object chosen now, so a creature the 4
 * damage killed (or that left and came back) is not stolen.
 */
val TheWrathOfKhan = card("The Wrath of Khan") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — This Saga deals 4 damage to target creature an opponent controls. At the beginning of the " +
        "next combat this turn, gain control of that creature until end of turn. Untap it. It gains " +
        "haste until end of turn.\n" +
        "II — You may exile a card from your hand. If you do, draw a card.\n" +
        "III — Each opponent sacrifices a nontoken permanent of their choice."

    sagaChapter(1) {
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.DealDamage(4, creature) then Effects.CreateDelayedTrigger(
            step = Step.BEGIN_COMBAT,
            timing = DelayedTriggerTiming.THIS_TURN_ONLY,
            effect = Effects.GainControl(creature, Duration.EndOfTurn) then
                Effects.Untap(creature) then
                Effects.GrantKeyword(Keyword.HASTE, creature),
        )
    }

    sagaChapter(2) {
        effect = Effects.May(
            Effects.IfYouDo(Patterns.Hand.exileFromHand(1, EffectTarget.Controller), Effects.DrawCards(1))
        )
    }

    sagaChapter(3) {
        effect = Effects.Sacrifice(
            filter = GameObjectFilter.Permanent.nontoken(),
            target = EffectTarget.PlayerRef(Player.EachOpponent),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "181"
        artist = "Kirk DouPonce"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/426d627e-bb88-4879-8d9f-db1babf22b15.jpg?1792510667"
    }
}
