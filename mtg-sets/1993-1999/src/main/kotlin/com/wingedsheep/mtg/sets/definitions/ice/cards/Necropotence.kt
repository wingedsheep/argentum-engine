package com.wingedsheep.mtg.sets.definitions.ice.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TurnPart
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SkipStepOrPhase
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerTiming
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Necropotence
 * {B}{B}{B}
 * Enchantment
 *
 * Skip your draw step.
 * Whenever you discard a card, exile that card from your graveyard.
 * Pay 1 life: Exile the top card of your library face down. Put that card into your hand at the
 * beginning of your next end step.
 *
 * Current Oracle wording (the original "If you discard a card, remove it from the game instead"
 * replacement was errata'd to the trigger above).
 *
 * Modelling notes:
 * - The skip is Necrodominance's static [SkipStepOrPhase] on the draw step.
 * - The discard trigger is Bag of Holding's shape without the link: `fromZone = GRAVEYARD` makes the
 *   exile a no-op when the card never reached, or already left, your graveyard (ruling — Obstinate
 *   Baloth, madness).
 * - The pay-life ability exiles the top card face down with no turn-up procedure
 *   ([FaceDownMode.HIDDEN], ruling: you can't look at it) and schedules a delayed trigger for your
 *   next end step ([DelayedTriggerTiming.NEXT_END_STEP] + `fireOnPlayer = You`, so activating during
 *   your end step defers to your following turn's). The exiled pile is carried into the delayed
 *   trigger, so only that card — if it is still exiled — goes to your hand; the trigger is
 *   independent of Necropotence and still fires if it has left the battlefield (ruling).
 */
val Necropotence = card("Necropotence") {
    manaCost = "{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Skip your draw step.\n" +
        "Whenever you discard a card, exile that card from your graveyard.\n" +
        "Pay 1 life: Exile the top card of your library face down. Put that card into your hand " +
        "at the beginning of your next end step."

    staticAbility {
        ability = SkipStepOrPhase(TurnPart.DRAW_STEP)
    }

    triggeredAbility {
        trigger = Triggers.you.discards()
        effect = Effects.Move(
            EffectTarget.TriggeringEntity,
            Zone.EXILE,
            fromZone = Zone.GRAVEYARD,
        )
        description = "Whenever you discard a card, exile that card from your graveyard."
    }

    activatedAbility {
        cost = Costs.PayLife(1)
        effect = Effects.Pipeline {
            val exiledFaceDown = gather(CardSource.TopOfLibrary(1))
            exile(exiledFaceDown, faceDown = FaceDownMode.HIDDEN)
            run(
                Effects.CreateDelayedTrigger(
                    step = Step.END,
                    fireOnPlayer = EffectTarget.PlayerRef(Player.You),
                    timing = DelayedTriggerTiming.NEXT_END_STEP,
                    effect = Effects.Pipeline {
                        toHand(exiledFaceDown)
                    },
                    carryCollections = listOf(exiledFaceDown.key),
                )
            )
        }
        description = "Pay 1 life: Exile the top card of your library face down. Put that card " +
            "into your hand at the beginning of your next end step."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "154"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/54d7a0c1-efb4-4a8d-ad92-a96d43835052.jpg?1783947497"
        ruling("2023-09-01", "You can't look at the cards exiled by the last ability while they're face down.")
        ruling("2017-11-17", "If a discarded card isn't put into your graveyard (due to an effect such as that of Obstinate Baloth) or leaves your graveyard (perhaps because another effect returned it to your hand), it won't be exiled.")
        ruling("2017-11-17", "If you discard a card with madness and wish to cast it, Necropotence's ability won't exile that card. If you don't wish to cast it, you choose whether it ends up exiled or in your graveyard.")
        ruling("2017-11-17", "Necropotence's last ability creates a delayed triggered ability that will put the exiled card into your hand. That ability still triggers even if Necropotence is removed from the battlefield before your end step.")
    }
}
