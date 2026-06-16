package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Assimilation Aegis
 * {1}{W}{U}
 * Artifact — Equipment
 * Equip {2}
 *
 * When this Equipment enters, exile up to one target creature until this Equipment leaves the
 * battlefield.
 * Whenever this Equipment becomes attached to a creature, for as long as this Equipment remains
 * attached to it, that creature becomes a copy of a creature card exiled with this Equipment.
 *
 * Modeling:
 * - The ETB exile reuses the linked-exile machinery ([Effects.ExileUntilLeaves] + a
 *   LeavesBattlefield trigger returning the linked card). "Up to one target creature" is an
 *   optional single target over any creature.
 * - The attach trigger is [Triggers.becomesAttached] with a SELF binding (it fires only when *this*
 *   Equipment becomes attached). Its payoff is [Effects.BecomeCopyOfLinkedExile]: the creature it
 *   just attached to ([EffectTarget.AttachedToTriggeringPermanent]) becomes a copy of the creature
 *   card exiled with this Equipment, lasting only for as long as the Equipment stays attached to it
 *   (the executor tags the copy so a state-based check reverts it on detach — CR 611.2b).
 * - If the ETB exile was declined (or the exiled card has left exile), the attach trigger finds no
 *   creature card to copy and the equipped creature keeps its own identity.
 */
val AssimilationAegis = card("Assimilation Aegis") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, exile up to one target creature until this Equipment " +
        "leaves the battlefield.\n" +
        "Whenever this Equipment becomes attached to a creature, for as long as this Equipment " +
        "remains attached to it, that creature becomes a copy of a creature card exiled with this " +
        "Equipment.\n" +
        "Equip {2}"

    // ETB: exile up to one target creature until this Equipment leaves the battlefield.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target(
            "up to one target creature",
            TargetCreature(count = 1, optional = true, filter = TargetFilter.Creature)
        )
        effect = Effects.ExileUntilLeaves(creature)
    }

    // LTB: return the exiled card to its owner's control.
    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    // On attach: the equipped creature becomes a copy of the exiled creature card while attached.
    triggeredAbility {
        trigger = Triggers.becomesAttached(binding = TriggerBinding.SELF)
        effect = Effects.BecomeCopyOfLinkedExile(EffectTarget.AttachedToTriggeringPermanent)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "192"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/0/1/014bf3c6-e46f-48f8-902f-82deeba260b2.jpg?1712356044"
    }
}
