package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CreatePermanentGlobalTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sarkhan, the Dragonspeaker - {3}{R}{R}
 * Legendary Planeswalker — Sarkhan
 * Starting Loyalty: 4
 *
 * +1: Until end of turn, Sarkhan, the Dragonspeaker becomes a legendary 4/4 red Dragon creature
 * with flying, indestructible, and haste. (He doesn't lose loyalty while he's not a planeswalker.)
 *
 * −3: Sarkhan, the Dragonspeaker deals 4 damage to target creature.
 *
 * −6: You get an emblem with "At the beginning of your draw step, draw two additional cards"
 * and "At the beginning of your end step, discard your hand."
 */
val SarkhanTheDragonspeaker = card("Sarkhan, the Dragonspeaker") {
    manaCost = "{3}{R}{R}"
    typeLine = "Legendary Planeswalker — Sarkhan"
    startingLoyalty = 4

    // +1: Become a 4/4 red Dragon creature with flying, indestructible, and haste
    loyaltyAbility(+1) {
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 4,
            toughness = 4,
            keywords = setOf(Keyword.FLYING, Keyword.INDESTRUCTIBLE, Keyword.HASTE),
            creatureTypes = setOf("Dragon"),
            removeTypes = setOf("PLANESWALKER"),
            colors = setOf("RED"),
            duration = Duration.EndOfTurn
        )
    }

    // -3: Deal 4 damage to target creature
    loyaltyAbility(-3) {
        val creature = target("creature", Targets.Creature)
        effect = Effects.DealDamage(4, creature)
    }

    // -6: Emblem with draw step and end step triggered abilities
    loyaltyAbility(-6) {
        effect = Effects.Composite(
            CreatePermanentGlobalTriggeredAbilityEffect(
                ability = TriggeredAbility.create(
                    trigger = Triggers.YourDrawStep.event,
                    binding = Triggers.YourDrawStep.binding,
                    effect = Effects.DrawCards(2)
                ),
                descriptionOverride = "At the beginning of your draw step, draw two additional cards."
            ),
            CreatePermanentGlobalTriggeredAbilityEffect(
                ability = TriggeredAbility.create(
                    trigger = Triggers.YourEndStep.event,
                    binding = Triggers.YourEndStep.binding,
                    effect = EffectPatterns.discardHand()
                ),
                descriptionOverride = "At the beginning of your end step, discard your hand."
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "119"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c58064fd-4d8b-4f54-812f-0bb1d7e2ddc2.jpg?1562793263"
        ruling("2014-09-20", "If Sarkhan becomes a creature due to his first ability, he's no longer a planeswalker. Damage dealt to him won't cause loyalty counters to be removed.")
        ruling("2014-09-20", "If Sarkhan becomes a creature, he doesn't count as a creature entering the battlefield. He was already on the battlefield; he only changed his types.")
    }
}
