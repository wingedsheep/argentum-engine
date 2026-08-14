package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.PayManaCostEffect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Edgar's Awakening — Innistrad: Crimson Vow #110.
 * {3}{B}{B}
 * Sorcery
 *
 * Return target creature card from your graveyard to the battlefield.
 * When you discard this card, you may pay {B}. When you do, return target creature card from
 * your graveyard to your hand.
 *
 * The discard half is a [Triggers.YouDiscardThis] ability — it functions from hand and fires as
 * the card is discarded, by which point the card itself is in the graveyard (so it is a legal
 * target of its own reflexive trigger). "You may pay {B}. When you do, …" is a genuine reflexive
 * trigger, not an "if you do" rider: the payment happens first, then a second ability goes on the
 * stack and picks its target, which is why [ReflexiveTriggerEffect] carries the target requirement
 * rather than the discard trigger.
 */
val EdgarsAwakening = card("Edgar's Awakening") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Return target creature card from your graveyard to the battlefield.\n" +
        "When you discard this card, you may pay {B}. When you do, return target creature card " +
        "from your graveyard to your hand."

    spell {
        val creature = target("target creature card from your graveyard", Targets.CreatureCardInYourGraveyard)
        effect = Effects.PutOntoBattlefield(creature)
    }

    triggeredAbility {
        trigger = Triggers.YouDiscardThis
        effect = ReflexiveTriggerEffect(
            action = PayManaCostEffect(ManaCost.parse("{B}")),
            reflexiveEffect = Effects.ReturnToHand(EffectTarget.ContextTarget(0)),
            reflexiveTargetRequirements = listOf(Targets.CreatureCardInYourGraveyard),
            // The composed description reads "return target to its owner's hand"; this is the
            // yes/no prompt the player actually sees, so spell the oracle wording out.
            descriptionOverride = "You may pay {B}. When you do, return target creature card " +
                "from your graveyard to your hand."
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "110"
        artist = "Joshua Raphael"
        flavorText = "\"Here you go. Can't have you sleeping through your own wedding day.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96ff9de6-f9ae-4b1c-9fd1-4ba9663922af.jpg?1783924865"
        ruling("2021-11-19", "You can't discard Edgar's Awakening just because you want to. In order to discard it, a rule or effect needs to allow or instruct you to do so.")
    }
}
