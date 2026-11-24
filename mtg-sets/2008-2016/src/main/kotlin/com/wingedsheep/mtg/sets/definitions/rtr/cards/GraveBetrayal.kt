package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EntersWithCharacteristics
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Grave Betrayal — Return to Ravnica #67 (only printing)
 * {5}{B}{B}
 * Enchantment
 * Whenever a creature you don't control dies, return it to the battlefield under your control with
 * an additional +1/+1 counter on it at the beginning of the next end step. That creature is a black
 * Zombie in addition to its other colors and types.
 *
 * - "You don't control" (not "an opponent controls"), so the trigger reads the dead creature's last
 *   known controller against `Not(ControlledByYou)`.
 * - The dead card is gathered by its triggering reference, which names it only while it is still the
 *   same object (CR 400.7): a token has ceased to exist and gathers nothing. The gathered card is
 *   carried into a step-based end-step delayed trigger (`carryCollections`); a carried card that has
 *   changed zones since — even one that left the graveyard and came back — is dropped when it fires,
 *   so it doesn't return (2012-10-01 ruling).
 * - The card returns under the delayed trigger's controller (the move's default) with one +1/+1
 *   counter as it enters, already a black Zombie in addition to its other colors and types
 *   ([EntersWithCharacteristics] adds and lasts while it stays on the battlefield).
 * - "The next end step" watches step *beginnings*, so a creature that dies during an end step waits
 *   for the following turn's end step.
 */
val GraveBetrayal = card("Grave Betrayal") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you don't control dies, return it to the battlefield under your control " +
        "with an additional +1/+1 counter on it at the beginning of the next end step. That creature is a " +
        "black Zombie in addition to its other colors and types."

    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.Creature.withControllerPredicate(
                ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
            )
        ).dies()
        effect = Effects.Pipeline {
            val deadCreature = gather(
                CardSource.Entity(EffectTarget.TriggeringEntity),
                name = "graveBetrayalDeadCreature"
            )
            run(Effects.CreateDelayedTrigger(
                step = Step.END,
                effect = Effects.Pipeline {
                    move(
                        deadCreature,
                        CardDestination.ToZone(Zone.BATTLEFIELD),
                        addCounterType = CounterType.PLUS_ONE_PLUS_ONE,
                        entersWith = EntersWithCharacteristics(
                            addedSubtypes = setOf(Subtype.ZOMBIE),
                            addedColors = setOf(Color.BLACK),
                        ),
                    )
                },
                carryCollections = listOf(deadCreature.key)
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "67"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47b38c68-8e72-4afc-bb5e-0b40880fdda9.jpg?1783940362"
        ruling("2012-10-01", "Grave Betrayal doesn't overwrite any previous colors or types. Rather, it adds another color and another creature type.")
        ruling("2012-10-01", "If the creature is normally colorless, it will simply be black. It won't be both black and colorless.")
        ruling("2012-10-01", "If a creature you don't control dies during the end step, that creature won't return to the battlefield until the beginning of the next end step.")
        ruling("2012-10-01", "If the creature card leaves the graveyard before the delayed triggered ability resolves, it won't return to the battlefield. This is also true if the card leaves the graveyard and returns to the graveyard before that ability resolves.")
        ruling("2012-10-01", "The phrase \"with an additional +1/+1 counter on it\" means that the creature enters with one more +1/+1 counter than it would normally enter with. It doesn't keep any counters it had on it when it died.")
    }
}
