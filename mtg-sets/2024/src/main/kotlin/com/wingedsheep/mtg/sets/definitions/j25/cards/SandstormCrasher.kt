package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ExertAsItAttacks
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sandstorm Crasher — Foundations Jumpstart #18 (canonical printing)
 * {3}{R} · Creature — Minotaur Berserker Wizard · 3/4
 *
 * Trample
 * You may exert this creature as it attacks. When you do, create a tapped and attacking token
 * that's a copy of target creature you control. Sacrifice the token at the beginning of the next
 * end step.
 *
 * The exert is [ExertAsItAttacks]; "when you do" is its linked self-exert trigger (CR 607.2h),
 * which targets as it goes on the stack before blockers. A bare "tapped and attacking" lets the
 * token's controller pick what it attacks as it enters (CR 508.4) —
 * [EnteringAttackDefender.CONTROLLER_CHOOSES]. "The next end step" is anyone's, so the sacrifice
 * isn't limited to the controller's turn.
 */
val SandstormCrasher = card("Sandstorm Crasher") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Minotaur Berserker Wizard"
    power = 3
    toughness = 4
    oracleText = "Trample\n" +
        "You may exert this creature as it attacks. When you do, create a tapped and attacking token " +
        "that's a copy of target creature you control. Sacrifice the token at the beginning of the next " +
        "end step. (An exerted creature won't untap during your next untap step.)"

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = ExertAsItAttacks()
    }

    triggeredAbility {
        trigger = Triggers.self.exertedAsItAttacks()
        val original = target(TargetFilter.CreatureYouControl)
        effect = Effects.CreateTokenCopyOfTarget(
            target = original,
            tapped = true,
            attacking = true,
            attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES,
            sacrificeAtStep = Step.END,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "18"
        artist = "Néstor Ossandón Leal"
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2bcf1ed6-5e5b-4035-9d21-f84ac4fc4fbd.jpg?1783908865"
        ruling("2024-11-08", "You can exert Sandstorm Crasher as you declare it as an attacking creature. You can't do so later in combat, and creatures put onto the battlefield attacking can't be exerted. Any abilities that trigger on exerting an attacking creature will resolve before blockers are declared.")
        ruling("2024-11-08", "The token created by the reflexive triggered ability copies exactly what was printed on the original creature and nothing else (unless that creature is copying something else or is a token; see below). It doesn't copy whether that creature is tapped or untapped, whether it has any counters on it or Auras and Equipment attached to it, or any non-copy effects that have changed its power, toughness, types, color, or so on. If it is a Vehicle, it is not crewed.")
        ruling("2024-11-08", "If the copied creature is a token, the token that's created copies the original characteristics of that token as stated by the effect that created the token, with the listed exceptions.")
        ruling("2024-11-08", "If the copied creature is copying something else, then the token enters as whatever that creature copied, with the listed exceptions.")
        ruling("2024-11-08", "Any enters abilities of the copied creature will trigger when the token enters. Any \"as [this permanent] enters\" or \"[this permanent] enters with\" abilities of the copied creature will also work.")
    }
}
