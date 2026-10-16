package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cheering Crowd
 * {1}{R/G}
 * Creature — Human Citizen, 2/2
 * At the beginning of each player's first main phase, that player may put a +1/+1 counter on
 * this creature. If they do, they add {C} for each counter on it.
 *
 * The trigger fires on every player's precombat (first) main phase — `Step.PRECOMBAT_MAIN`
 * scoped to [Player.Each] — so "that player" is the active player, which can be an opponent.
 * That player, not this creature's controller, both makes the "may" choice and receives the
 * mana. We rebind the resolution controller to the triggering (active) player by wrapping the
 * body in `ForEachPlayer(Player.TriggeringPlayer, …)`: a single-player iteration whose only
 * effect is to bind `controllerId` to that player, so the default `Effects.May` decision-maker is
 * that player and the colorless mana lands in that player's pool. The +1/+1 counter always goes
 * on this creature ([EffectTarget.Self] is unaffected by the controller rebind), and the {C}
 * amount counts every counter on it (any kind, including the one just placed).
 *
 * "If they do" is an [Effects.IfYouDo] gated on [SuccessCriterion.CountersAdded]: if the Crowd has
 * left the battlefield by resolution, no counter can be placed, so no mana is added — without the
 * gate the {C} count would fall back to the Crowd's last-known counters. The yes/no `prompt` is
 * phrased to the deciding player; the ability's `description` keeps the Oracle wording for stack
 * text (a `descriptionOverride` on the `May` would replace the prompt too).
 */
val CheeringCrowd = card("Cheering Crowd") {
    manaCost = "{1}{R/G}"
    colorIdentity = "RG"
    typeLine = "Creature — Human Citizen"
    power = 2
    toughness = 2
    oracleText = "At the beginning of each player's first main phase, that player may put a +1/+1 " +
        "counter on this creature. If they do, they add {C} for each counter on it."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.PRECOMBAT_MAIN)
        description = "At the beginning of each player's first main phase, that player may put a +1/+1 " +
            "counter on this creature. If they do, they add {C} for each counter on it."
        effect = Effects.ForEachPlayer(
            Player.TriggeringPlayer,
            listOf(
                Effects.May(
                    Effects.IfYouDo(
                        Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                        Effects.AddColorlessMana(DynamicAmounts.countersOnSelf(null)),
                        successCriterion = SuccessCriterion.CountersAdded
                    ),
                    prompt = "Put a +1/+1 counter on Cheering Crowd? If you do, add {C} for each counter on it."
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "126"
        artist = "Kim Sokol"
        flavorText = "\"We love you, Spider-Man!\""
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5fbce72f-e9a1-4d9f-b9b3-24dbafeef841.jpg?1783905320"
    }
}
