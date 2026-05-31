package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Faller's Faithful
 * {2}{B}
 * Creature — Human Wizard
 * 3/1
 *
 * When this creature enters, destroy up to one other target creature. If that creature
 * wasn't dealt damage this turn, its controller draws two cards.
 *
 * The conditional draw is scheduled before the destroy in the composite so the
 * `WasDealtDamageThisTurn` history component can still be read off the target — the
 * component is stripped when a permanent leaves the battlefield. Functionally
 * equivalent to the oracle ordering: damage history is a turn-long fact unaffected
 * by destroy, and "its controller" is captured from the still-on-battlefield target.
 */
val FallersFaithful = card("Faller's Faithful") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Wizard"
    power = 3
    toughness = 1
    oracleText = "When this creature enters, destroy up to one other target creature. " +
        "If that creature wasn't dealt damage this turn, its controller draws two cards."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target(
            "up to one other target creature",
            TargetCreature(optional = true, filter = TargetFilter.OtherCreature)
        )
        effect = ConditionalEffect(
            condition = Conditions.TargetMatchesFilter(
                GameObjectFilter.Creature.copy(
                    statePredicates = listOf(StatePredicate.Not(StatePredicate.WasDealtDamageThisTurn))
                )
            ),
            effect = Effects.DrawCards(2, EffectTarget.TargetController)
        ).then(Effects.Destroy(creature))
        description = "When this creature enters, destroy up to one other target creature. " +
            "If that creature wasn't dealt damage this turn, its controller draws two cards."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "100"
        artist = "Lie Setiawan"
        flavorText = "\"Death is a trick of the light. Snuff it out and discover immortality.\"\n—Plummet Record 170.312"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cbb1b46f-72e0-4c2f-8012-74529bd29a0d.jpg?1752946961"
    }
}
