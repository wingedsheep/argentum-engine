package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetChooser
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Confusion in the Ranks — Mirrodin #87 (canonical printing)
 * {3}{R}{R} · Enchantment · Rare
 *
 * Whenever an artifact, creature, or enchantment enters, its controller chooses target permanent
 * another player controls that shares a card type with it. Exchange control of those permanents.
 *
 * Modelling notes:
 * - **The chooser is not the controller.** "Its controller chooses" hands the selection to whoever
 *   played the permanent that entered — which for the Fountain-style routing is
 *   [TargetChooser.ControllerOfTriggeringEntity], not `TriggeringPlayer`: an enters trigger's
 *   triggering entity is the *permanent*, so the deciding player has to be read off it. The target
 *   remains a target of Confusion's own ability (CR 115 legality, respondable) — only who picks it
 *   changes.
 * - "Another player" is relative to the **entering permanent's** controller, not to Confusion's, so
 *   it's `Not(ControlledByReferencedPlayer(ControllerOfTriggeringEntity))` rather than
 *   `opponentControls()`. Getting this wrong is invisible in a two-player game and wrong in every
 *   larger one: with Confusion out, a permanent entering under player A can be swapped with one
 *   under B or C, and never with another of A's own.
 * - "Shares a card type with it" is the new [com.wingedsheep.sdk.scripting.predicates.CardPredicate]
 *   `SharesCardTypeWith` over [EntityReference.Triggering]. Both sides read projected types, so an
 *   animated artifact land that enters can be swapped for a creature. Card types only — two
 *   *legendary* permanents don't share a card type by being legendary.
 * - **It triggers on itself.** Confusion in the Ranks is an enchantment, so its own arrival meets
 *   its own trigger (`TriggerBinding.ANY` includes the source) and you must swap it for an
 *   opponent's artifact, creature, or enchantment if one is out. That is the printed card, and the
 *   reason it reads as a symmetrical mess rather than an engine.
 * - The trigger is mandatory: with no legal target it is simply removed from the stack (CR 603.3d)
 *   rather than resolving as a no-op, so a lone permanent entering an otherwise empty board does
 *   nothing.
 * - Only one target is named. The entering permanent is the *other* half of the exchange and is
 *   referenced through [EffectTarget.TriggeringEntity]; it is not a second target, exactly as
 *   printed.
 */
val ConfusionInTheRanks = card("Confusion in the Ranks") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever an artifact, creature, or enchantment enters, its controller chooses " +
        "target permanent another player controls that shares a card type with it. Exchange " +
        "control of those permanents."

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.ArtifactCreatureOrEnchantment,
            binding = TriggerBinding.ANY
        )
        val swapped = target(
            "permanent another player controls that shares a card type with it",
            TargetObject(
                filter = TargetFilter(
                    GameObjectFilter.Permanent
                        .withControllerPredicate(
                            ControllerPredicate.Not(
                                ControllerPredicate.ControlledByReferencedPlayer(
                                    EffectTarget.ControllerOfTriggeringEntity
                                )
                            )
                        )
                        .sharingCardTypeWith(EntityReference.Triggering)
                ),
                chooser = TargetChooser.ControllerOfTriggeringEntity
            )
        )
        effect = Effects.ExchangeControl(EffectTarget.TriggeringEntity, swapped)
        description = "Whenever an artifact, creature, or enchantment enters, its controller " +
            "chooses target permanent another player controls that shares a card type with it. " +
            "Exchange control of those permanents."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "87"
        artist = "Ron Spencer"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d46aca2-4714-4d2c-9466-5f1c043b8726.jpg"
    }
}
