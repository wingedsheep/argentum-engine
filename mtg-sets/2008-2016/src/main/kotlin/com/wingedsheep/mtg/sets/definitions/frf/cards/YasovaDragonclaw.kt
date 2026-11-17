package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Yasova Dragonclaw — Fate Reforged #148 (canonical printing)
 * {2}{G} · Legendary Creature — Human Warrior · 4/2
 *
 * Trample
 * At the beginning of combat on your turn, you may pay {1}{U/R}{U/R}. If you do, gain control of
 * target creature an opponent controls with power less than Yasova Dragonclaw's power until end of
 * turn, untap that creature, and it gains haste until end of turn.
 *
 * The target is chosen as the trigger goes on the stack; the {1}{U/R}{U/R} is offered once on
 * resolution ([Effects.MayPay]). "Power less than Yasova's power" is the strict
 * `powerLessThanEntity(Self)` comparison against Yasova's projected power (last-known power if she
 * has left), so it is checked both when the trigger is put on the stack and as it resolves — a target
 * whose power has caught up by then is illegal and the ability does nothing (ruling).
 */
val YasovaDragonclaw = card("Yasova Dragonclaw") {
    manaCost = "{2}{G}"
    colorIdentity = "GUR"
    typeLine = "Legendary Creature — Human Warrior"
    power = 4
    toughness = 2
    oracleText = "Trample\n" +
        "At the beginning of combat on your turn, you may pay {1}{U/R}{U/R}. If you do, gain control of " +
        "target creature an opponent controls with power less than Yasova Dragonclaw's power until end " +
        "of turn, untap that creature, and it gains haste until end of turn."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val creature = target(
            TargetFilter(GameObjectFilter.Creature.opponentControls().powerLessThanEntity(EffectTarget.Self))
        )
        effect = Effects.MayPay(
            cost = ManaCost.parse("{1}{U/R}{U/R}"),
            then = Effects.GainControl(creature, Duration.EndOfTurn) then
                Effects.Untap(creature) then
                Effects.GrantKeyword(Keyword.HASTE, creature)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "148"
        artist = "Winona Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/490994c6-9fef-482b-835d-a64350bfaa8d.jpg?1783938674"

        ruling("2014-11-24", "The powers of the target creature and Yasova Dragonclaw are checked as you put the ability on the stack and again as the ability resolves. Once the ability resolves, it doesn't matter what happens to either creature's power.")
        ruling("2014-11-24", "Gaining control of a creature doesn't cause you to gain control of any Auras or Equipment attached to it.")
    }
}
