package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate

/**
 * Rigo, Streetwise Mentor — Streets of New Capenna #215.
 *
 * The attack trigger is the once-per-combat "whenever you attack" [Triggers.you.attacks] with an
 * attacker filter of power 1 or less (read off projected state at declaration). "A player or
 * planeswalker" carves out battles: an attacker counts only if it isn't attacking a battle — the same
 * `Not(IsAttackingABattle)` carve-out as Tenuous Truce. Attacks split across several players and
 * planeswalkers still trigger once, since "you attack" is a single event per combat.
 */
val RigoStreetwiseMentor = card("Rigo, Streetwise Mentor") {
    manaCost = "{G/W}{W}{W/U}"
    colorIdentity = "GWU"
    typeLine = "Legendary Creature — Cat Citizen"
    power = 2
    toughness = 2
    oracleText = "Rigo enters with a shield counter on it. (If it would be dealt damage or destroyed, remove a " +
        "shield counter from it instead.)\n" +
        "Whenever you attack a player or planeswalker with one or more creatures with power 1 or less, draw a card."

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.SHIELD,
            count = 1,
            selfOnly = true,
        )
    )

    triggeredAbility {
        trigger = Triggers.you.attacks(
            GameObjectFilter.Creature.powerAtMost(1)
                .withStatePredicate(StatePredicate.Not(StatePredicate.IsAttackingABattle))
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "215"
        artist = "Scott Murphy"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67c6aef6-d846-4e02-a5f9-6eb0b2212208.jpg?1783923073"
    }
}
