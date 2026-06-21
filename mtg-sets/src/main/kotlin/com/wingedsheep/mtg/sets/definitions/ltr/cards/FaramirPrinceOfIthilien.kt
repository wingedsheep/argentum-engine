package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerTiming
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Faramir, Prince of Ithilien
 * {2}{W}{U}
 * Legendary Creature — Human Noble
 * 3/3
 *
 * At the beginning of your end step, choose an opponent. At the beginning of that
 * player's next end step, you draw a card if they didn't attack you that turn.
 * Otherwise, create three 1/1 white Human Soldier creature tokens.
 *
 * Modelled as: at the controller's end step, an opponent is chosen. The choice is
 * expressed via [Targets.Opponent] (the engine's mechanism for "choose an opponent");
 * functionally this is the controller picking one opponent. That chosen opponent is
 * baked into [CreateDelayedTriggerEffect.fireOnPlayer] so the delayed trigger fires
 * at the beginning of *that* player's next end step and re-exposes them as
 * [Player.TriggeringPlayer] to the inner conditional.
 *
 * When the delayed trigger resolves, it checks whether the chosen opponent attacked
 * Faramir's controller this turn (CR 508.6 — a player "has attacked [a player]" if they
 * declared one or more creatures as attackers attacking that player). Attacking a
 * planeswalker or battle the controller owns is not attacking the controller, so it
 * doesn't count here. If they did NOT, the controller draws a card; otherwise the
 * controller creates three 1/1 white Human Soldier tokens.
 */
val FaramirPrinceOfIthilien = card("Faramir, Prince of Ithilien") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human Noble"
    power = 3
    toughness = 3
    oracleText = "At the beginning of your end step, choose an opponent. At the beginning " +
        "of that player's next end step, you draw a card if they didn't attack you that turn. " +
        "Otherwise, create three 1/1 white Human Soldier creature tokens."

    triggeredAbility {
        trigger = Triggers.YourEndStep
        target = Targets.Opponent
        effect = CreateDelayedTriggerEffect(
            step = Step.END,
            fireOnPlayer = EffectTarget.ContextTarget(0),
            // The chosen opponent is never the active player at Faramir's controller's end step,
            // so the next END step gated to that opponent (fireOnPlayer) is necessarily a later
            // turn — CURRENT_TURN_OR_LATER avoids a turn-floor off-by-one while still firing only
            // at "that player's next end step" (same axis as Nafs Asp).
            timing = DelayedTriggerTiming.CURRENT_TURN_OR_LATER,
            effect = ConditionalEffect(
                condition = Conditions.Not(
                    Conditions.PlayerAttackedPlayerThisTurn(
                        attacker = Player.TriggeringPlayer,
                        defender = Player.You
                    )
                ),
                effect = Effects.DrawCards(1),
                elseEffect = Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.WHITE),
                    creatureTypes = setOf("Human", "Soldier"),
                    count = 3,
                    imageUri = "https://cards.scryfall.io/normal/front/a/6/a6181330-7521-4ec6-be6c-b35487c2d2d4.jpg?1699974464"
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "202"
        artist = "Tomas Duchek"
        flavorText = "The City was made more fair than it had ever been, even in the days of its first glory."
        imageUri = "https://cards.scryfall.io/normal/front/8/7/8700923a-e9ff-4ced-87fe-1ab26554623a.jpg?1686969755"
    }
}
