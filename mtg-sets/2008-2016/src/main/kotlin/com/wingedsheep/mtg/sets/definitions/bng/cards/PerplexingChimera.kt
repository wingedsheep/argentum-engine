package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Perplexing Chimera — Born of the Gods #48 (canonical printing)
 * {4}{U}
 * Enchantment Creature — Chimera
 * 3/3
 * Whenever an opponent casts a spell, you may exchange control of this creature and that spell.
 * If you do, you may choose new targets for the spell.
 *
 * `ExchangeControl` takes a spell on the stack as either side: the Chimera changes hands through a
 * Layer.CONTROL effect that lasts indefinitely, the spell by a rewrite of its stack object's
 * controller (so a permanent spell enters under you). The exchange fails whole if the Chimera has
 * left the battlefield or the spell the stack (CR 701.12a), and `SuccessCriterion.ControlChanged`
 * then skips the retarget. The retarget follows the control change, so new targets are judged
 * from your perspective, and its chooser is the ability's controller, not the spell's (last ruling).
 */
val PerplexingChimera = card("Perplexing Chimera") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment Creature — Chimera"
    power = 3
    toughness = 3
    oracleText = "Whenever an opponent casts a spell, you may exchange control of this creature and that spell. " +
        "If you do, you may choose new targets for the spell. (If the spell becomes a permanent, you control that permanent.)"

    triggeredAbility {
        trigger = Triggers.anOpponent.casts()
        effect = Effects.May(
            Effects.IfYouDo(
                action = Effects.ExchangeControl(EffectTarget.Self, EffectTarget.TriggeringEntity),
                then = Effects.ChangeTriggeringObjectTargets(spell = EffectTarget.TriggeringEntity),
                successCriterion = SuccessCriterion.ControlChanged,
            )
        )
        description = "Whenever an opponent casts a spell, you may exchange control of this creature and that spell. " +
            "If you do, you may choose new targets for the spell."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "48"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9cff40b-9cae-47d0-8df4-c287a17a33e4.jpg?1783939566"
        ruling("2014-02-01", "You may exchange control of Perplexing Chimera and any spell cast by an opponent, not just one with targets.")
        ruling("2014-02-01", "You make the decision whether to exchange control of Perplexing Chimera and the spell as the triggered ability resolves.")
        ruling("2014-02-01", "If Perplexing Chimera leaves the battlefield or the spell leaves the stack before the triggered ability resolves, you can't make the exchange.")
        ruling("2014-02-01", "Neither Perplexing Chimera nor the spell changes zones. Only control of them is exchanged.")
        ruling("2014-02-01", "After the ability resolves, you control the spell. Any instance of \"you\" in that spell's text now refers to you, \"an opponent\" refers to one of your opponents, and so on. The change of control happens before new targets are chosen, so any targeting restrictions such as \"target opponent\" or \"target creature you control\" are now made in reference to you, not the spell's original controller.")
        ruling("2014-02-01", "You may change any of the spell's targets. If you change a target, you must choose a legal target for the spell. If you can't, you must leave the target the same (even if that target is now illegal).")
        ruling("2014-02-01", "Gaining control of a spell and changing its targets won't cause any heroic abilities of the new targets to trigger.")
        ruling("2014-02-01", "If you gain control of an instant or sorcery spell, it will be put into its owner's graveyard as it resolves, fails to resolve, or is countered.")
        ruling("2014-02-01", "In some unusual cases, you may not control Perplexing Chimera when its triggered ability resolves. In these cases, you can exchange control of Perplexing Chimera and the spell that causes the ability to trigger, even if you control neither of them. If you do, you'll be able to change targets of the spell, not the spell's new controller.")
    }
}
