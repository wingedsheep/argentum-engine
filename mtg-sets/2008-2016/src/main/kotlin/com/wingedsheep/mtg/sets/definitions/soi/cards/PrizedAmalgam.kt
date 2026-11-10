package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private const val PRIZED_AMALGAM_TEXT =
    "Whenever a creature enters, if it entered from your graveyard or you cast it from your " +
        "graveyard, return this card from your graveyard to the battlefield tapped at the beginning " +
        "of the next end step."

/**
 * Prized Amalgam — Shadows over Innistrad #249 (canonical printing)
 * {1}{U}{B} · Creature — Zombie · 3/3
 *
 * The printed ability functions only from the graveyard (`triggerZone = GRAVEYARD`) and watches
 * *any* creature entering, under anyone's control. Its two disjoint halves are scripted as two
 * triggered abilities so each reads exactly what the Oracle text says:
 *  - **"it entered from your graveyard"** — a creature you *own* (a card is only ever in its owner's
 *    graveyard) put onto the battlefield straight from a graveyard, whoever controls it now (an
 *    opponent's reanimation spell on your creature card counts). `Not(TriggeringEntityWasCast)`
 *    keeps this half off the cast branch, so one creature never fires both.
 *  - **"you cast it from your graveyard"** — a creature you own *and control* that resolved from a
 *    spell cast from a graveyard. A resolving permanent spell enters under its caster's control, so
 *    the trigger-time control check is "you cast it"; an opponent casting your card from your
 *    graveyard doesn't count, and stealing the creature afterwards doesn't undo the trigger (the
 *    filter is only read as the event happens).
 *
 * Both are intervening-if triggers (CR 603.4) that schedule a delayed "next end step" return of
 * the Amalgam itself. The delayed trigger carries the Amalgam's identity, and the return is
 * graveyard-guarded, so an Amalgam that left the graveyard before the end step stays where it went
 * (CR 400.7). Scheduled during an end step, the return waits for the following turn's end step.
 */
val PrizedAmalgam = card("Prized Amalgam") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Zombie"
    power = 3
    toughness = 3
    oracleText = PRIZED_AMALGAM_TEXT

    val returnAtNextEndStep = Effects.CreateDelayedTrigger(
        step = Step.END,
        effect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.Self, tapped = true),
    )

    // "…if it entered from your graveyard…"
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.ownedByYou()).enters()
        triggerZone = Zone.GRAVEYARD
        interveningIf = Conditions.All(
            Conditions.TriggeringEntityEnteredOrWasCastFromGraveyard,
            Conditions.Not(Conditions.TriggeringEntityWasCast),
        )
        effect = returnAtNextEndStep
        description = PRIZED_AMALGAM_TEXT
    }

    // "…or you cast it from your graveyard…"
    triggeredAbility {
        // `withControllerPredicate` replaces rather than combines, so owner and controller are
        // joined explicitly (as on Grist, Voracious Larva).
        trigger = Triggers.a(
            GameObjectFilter.Creature.withControllerPredicate(
                ControllerPredicate.And(listOf(ControllerPredicate.ControlledByYou, ControllerPredicate.OwnedByYou))
            )
        ).enters()
        triggerZone = Zone.GRAVEYARD
        interveningIf = Conditions.TriggeringEntityWasCastFrom(Zone.GRAVEYARD)
        effect = returnAtNextEndStep
        description = PRIZED_AMALGAM_TEXT
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "249"
        artist = "Karl Kopinski"
        flavorText = "\"Ludevic's laboratory is a veritable wonderland. I have never felt so inspired.\"\n" +
            "—Stitcher Geralf"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/634cedb6-8b00-4f4b-8790-541188955295.jpg?1783937712"
        ruling("2021-03-19", "A card cast using its madness ability is cast from exile, not from your graveyard.")
        ruling(
            "2021-03-19",
            "Prized Amalgam's ability triggers only if it's in your graveyard immediately after an appropriate " +
                "creature enters the battlefield. A Prized Amalgam that's already on the battlefield won't be " +
                "returned at the beginning of the next end step if it's put into your graveyard later."
        )
    }
}
