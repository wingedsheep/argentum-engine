package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect

/**
 * Season of the Witch
 * {B}{B}{B}
 * Enchantment
 * At the beginning of your upkeep, sacrifice this enchantment unless you pay 2 life.
 * At the beginning of the end step, destroy all untapped creatures that didn't attack this turn,
 * except for creatures that couldn't attack.
 *
 * The upkeep half is the plain `PayOrSuffer` ransom. The end-step half is one `DestroyAll` over a
 * filter that spells the printed sentence out clause by clause: untapped, didn't attack this turn,
 * and — the exemption — could have attacked in the first place.
 *
 * "Couldn't attack" is [GameObjectFilter.couldHaveAttackedThisTurn]'s negation. Its first and
 * broadest clause is the one that decides who the sweep hits at all: only the active player
 * declares attackers (CR 508.1a), so a creature an opponent controls could not have attacked this
 * turn and is exempt. The sweep is therefore one-sided in practice — on your turn it destroys the
 * creatures *you* left at home, not your opponent's board. It also exempts everyone when the turn
 * had no Declare Attackers Step to skip, which is what an opposing False Peace or Fatespinner
 * produces. Per-creature, it exempts defender, "can't attack", and summoning sickness. It is
 * deliberately not the full declare-attackers legality check (that needs a chosen defending player
 * and a card registry, neither of which predicate evaluation has), so a creature kept home only by
 * a card-specific "can't attack unless …" restriction is still destroyed.
 *
 * The trigger is `EachEndStep`, not `YourEndStep`: the printed line says "the end step", so the
 * opponent's own stay-at-home creatures are judged on the opponent's turn.
 */
val SeasonOfTheWitch = card("Season of the Witch") {
    manaCost = "{B}{B}{B}"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, sacrifice this enchantment unless you pay 2 " +
        "life.\nAt the beginning of the end step, destroy all untapped creatures that didn't " +
        "attack this turn, except for creatures that couldn't attack."

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = PayOrSufferEffect(
            cost = Costs.pay.PayLife(2),
            suffer = SacrificeSelfEffect,
        )
        description = "At the beginning of your upkeep, sacrifice this enchantment unless you pay 2 life."
    }

    triggeredAbility {
        trigger = Triggers.EachEndStep
        effect = Effects.DestroyAll(
            GameObjectFilter.Creature
                .untapped()
                .didntAttackThisTurn()
                .couldHaveAttackedThisTurn()
        )
        description = "At the beginning of the end step, destroy all untapped creatures that " +
            "didn't attack this turn, except for creatures that couldn't attack."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "52"
        artist = "Jesper Myrfors"
        imageUri = "https://cards.scryfall.io/normal/front/0/6/06900a71-34ca-48c6-94ac-fca744356829.jpg?1783947938"
    }
}
