package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.training
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Gryffwing Cavalry
 * {3}{W}
 * Creature — Human Knight
 * 2/2
 * Flying
 * Training (Whenever this creature attacks with another creature with greater power, put a
 * +1/+1 counter on this creature.)
 * Whenever this creature attacks, you may pay {1}{W}. If you do, target attacking creature
 * without flying gains flying until end of turn.
 *
 * Three pieces:
 *  - [Keyword.FLYING] evasion plus [training] (keyword badge + the +1/+1 attack trigger).
 *  - A hand-written attack trigger whose payoff is gated behind an optional mana payment
 *    ([MayPayManaEffect] — the "you may pay {1}{W}. If you do" reflexive, CR 603.12): pay {1}{W}
 *    to grant flying until end of turn to a chosen attacking creature that doesn't already have
 *    it. The target filter is `AttackingCreature.withoutKeyword(FLYING)` so only a grounded
 *    attacker is a legal target (matching "target attacking creature without flying"), and the
 *    Cavalry itself — which has flying — is never a legal target for its own trigger.
 *
 * Both attack triggers (Training's counter and this reflexive) go on the stack when the Cavalry
 * attacks; their order is chosen by the controller per CR 603.3b.
 */
val GryffwingCavalry = card("Gryffwing Cavalry") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "Training (Whenever this creature attacks with another creature with greater power, " +
        "put a +1/+1 counter on this creature.)\n" +
        "Whenever this creature attacks, you may pay {1}{W}. If you do, target attacking " +
        "creature without flying gains flying until end of turn."

    keywords(Keyword.FLYING)
    training()

    triggeredAbility {
        trigger = Triggers.Attacks
        val flyer = target(
            "target attacking creature without flying",
            TargetCreature(filter = TargetFilter.AttackingCreature.withoutKeyword(Keyword.FLYING)),
        )
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{1}{W}"),
            effect = Effects.GrantKeyword(Keyword.FLYING, flyer),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "16"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/7/9/792d5b41-27f3-455b-890e-a1771944fc7d.jpg?1783924920"
    }
}
