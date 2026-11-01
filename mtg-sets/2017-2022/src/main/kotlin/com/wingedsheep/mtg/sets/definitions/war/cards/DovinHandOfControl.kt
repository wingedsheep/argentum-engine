package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Dovin, Hand of Control
 * {2}{W/U}
 * Legendary Planeswalker — Dovin
 * Loyalty 5
 *
 * Artifact, instant, and sorcery spells your opponents cast cost {1} more to cast.
 * −1: Until your next turn, prevent all damage that would be dealt to and dealt by target
 * permanent an opponent controls.
 *
 * The −1 is two all-damage shields on the same target, both lasting until your next turn: one on
 * damage dealt to it, one on damage dealt by it. (`PreventionDirection.Both` is the combat-only
 * Maze of Ith shield, so it can't carry the noncombat half.)
 */
val DovinHandOfControl = card("Dovin, Hand of Control") {
    manaCost = "{2}{W/U}"
    colorIdentity = "WU"
    typeLine = "Legendary Planeswalker — Dovin"
    startingLoyalty = 5
    oracleText = "Artifact, instant, and sorcery spells your opponents cast cost {1} more to cast.\n" +
        "−1: Until your next turn, prevent all damage that would be dealt to and dealt by target permanent an opponent controls."

    // Artifact, instant, and sorcery spells your opponents cast cost {1} more to cast.
    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.OpponentsCast(
                GameObjectFilter(
                    cardPredicates = listOf(
                        CardPredicate.Or(
                            listOf(CardPredicate.IsArtifact, CardPredicate.IsInstant, CardPredicate.IsSorcery)
                        )
                    )
                )
            ),
            modification = CostModification.IncreaseGeneric(1),
        )
    }

    // −1: Until your next turn, prevent all damage that would be dealt to and dealt by target
    // permanent an opponent controls.
    loyaltyAbility(-1) {
        val permanent = target(TargetFilter.PermanentOpponentControls)
        effect = Effects.PreventDamage(target = permanent, duration = Duration.UntilYourNextTurn) then
            Effects.PreventAllDamageDealtBy(permanent, duration = Duration.UntilYourNextTurn)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "229"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd6ff745-919b-4688-9e9e-ab7835b3b891.jpg?1783933381"
    }
}
