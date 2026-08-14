package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ReplaceDamageWithCounters
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Anti-Venom, Horrifying Healer — Marvel's Spider-Man #1
 * {W}{W}{W}{W}{W} · Legendary Creature — Symbiote Hero · 5/5
 *
 * When Anti-Venom enters, if he was cast, return target creature card from your graveyard to
 * the battlefield.
 * If damage would be dealt to Anti-Venom, prevent that damage and put that many +1/+1 counters
 * on him.
 *
 * The damage-to-counters clause is a `RecipientFilter.Self` `ReplaceDamageWithCounters`, now wired
 * on the creature-damage paths (`CombatDamageManager.applyDamageToCreature` +
 * `DamageUtils.applyDamage`).
 */
val AntiVenomHorrifyingHealer = card("Anti-Venom, Horrifying Healer") {
    manaCost = "{W}{W}{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Symbiote Hero"
    power = 5
    toughness = 5
    oracleText = "When Anti-Venom enters, if he was cast, return target creature card from your " +
        "graveyard to the battlefield.\n" +
        "If damage would be dealt to Anti-Venom, prevent that damage and put that many +1/+1 " +
        "counters on him."

    // When Anti-Venom enters, if he was cast, reanimate a creature from your graveyard.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.WasCast
        val creature = target("target creature card", Targets.CreatureCardInYourGraveyard)
        effect = Effects.PutOntoBattlefield(creature)
    }

    // If damage would be dealt to Anti-Venom, prevent it and put that many +1/+1 counters on him.
    replacementEffect(
        ReplaceDamageWithCounters(
            counterType = Counters.PLUS_ONE_PLUS_ONE,
            appliesTo = EventPattern.DamageEvent(recipient = RecipientFilter.Self)
        )
    )

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1"
        artist = "Néstor Ossandón Leal"
        flavorText = "\"You are a sickness on this city. I am the cure!\""
        imageUri = "https://cards.scryfall.io/normal/front/5/6/560384fe-7be0-4b93-a515-2fe687ab2492.jpg?1783905365"
    }
}
