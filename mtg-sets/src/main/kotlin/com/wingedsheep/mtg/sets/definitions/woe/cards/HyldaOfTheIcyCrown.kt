package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.PayManaCostEffect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hylda of the Icy Crown
 * {2}{W}{U}
 * Legendary Creature — Human Warlock
 * 3/4
 *
 * Whenever you tap an untapped creature an opponent controls, you may pay {1}. When you do,
 * choose one —
 * • Create a 4/4 white and blue Elemental creature token.
 * • Put a +1/+1 counter on each creature you control.
 * • Scry 2, then draw a card.
 *
 * [Triggers.YouTap] carries the *attribution* half — only a tap Hylda's controller caused fires
 * this, so an opponent tapping their own creature (attacking, crewing, paying a cost) does nothing,
 * and neither does a spell you control that instructs *them* to tap (Tangle Wire). "Untapped" is
 * intrinsic: tapping is a transition (CR 603.2f), so an already-tapped creature emits no tap event.
 *
 * The payoff is a "When you do" reflexive (CR 603.12): the optional {1} is the action, and the mode
 * is chosen only once it is paid ([ReflexiveTriggerEffect] over a resolution-time
 * [ModalEffect.chooseOne] — none of the three modes targets). Per-tap, not once per turn: tapping
 * two of an opponent's creatures at once triggers it twice, each with its own {1}.
 */
val HyldaOfTheIcyCrown = card("Hylda of the Icy Crown") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human Warlock"
    power = 3
    toughness = 4
    oracleText = "Whenever you tap an untapped creature an opponent controls, you may pay {1}. " +
        "When you do, choose one —\n" +
        "• Create a 4/4 white and blue Elemental creature token.\n" +
        "• Put a +1/+1 counter on each creature you control.\n" +
        "• Scry 2, then draw a card."

    triggeredAbility {
        trigger = Triggers.YouTap(GameObjectFilter.Creature.opponentControls())
        effect = ReflexiveTriggerEffect(
            action = PayManaCostEffect(ManaCost.parse("{1}")),
            optional = true,
            reflexiveEffect = ModalEffect.chooseOne(
                Mode.noTarget(
                    Effects.CreateToken(
                        power = 4,
                        toughness = 4,
                        colors = setOf(Color.WHITE, Color.BLUE),
                        creatureTypes = setOf("Elemental"),
                    ),
                    "Create a 4/4 white and blue Elemental creature token"
                ),
                Mode.noTarget(
                    Effects.ForEachInGroup(
                        filter = GroupFilter(GameObjectFilter.Creature.youControl()),
                        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
                    ),
                    "Put a +1/+1 counter on each creature you control"
                ),
                Mode.noTarget(
                    Patterns.Library.scry(2).then(Effects.DrawCards(1)),
                    "Scry 2, then draw a card"
                ),
            ),
            descriptionOverride = "you may pay {1}. When you do, choose one — create a 4/4 white " +
                "and blue Elemental creature token; put a +1/+1 counter on each creature you " +
                "control; or scry 2, then draw a card"
        )
        description = "Whenever you tap an untapped creature an opponent controls, you may pay {1}. " +
            "When you do, choose one — create a 4/4 white and blue Elemental creature token; put a " +
            "+1/+1 counter on each creature you control; or scry 2, then draw a card."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "206"
        artist = "Ekaterina Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae9231fd-053d-4b84-a7a8-86063465bc49.jpg?1783915071"
        ruling(
            "2023-09-01",
            "Hylda of the Icy Crown's ability will trigger only when an effect instructs you to tap " +
                "an opponent's creature. It won't trigger if a spell or ability you control instructs " +
                "an opponent to tap a creature they control. For example, if you control Tangle Wire " +
                "and an opponent taps an untapped creature they control as part of the resolution of " +
                "Tangle Wire's triggered ability, Hylda's ability won't trigger."
        )
    }
}
