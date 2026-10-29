package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ugin, the Spirit Dragon
 * {8}
 * Legendary Planeswalker — Ugin
 * +2: Ugin deals 3 damage to any target.
 * −X: Exile each permanent with mana value X or less that's one or more colors.
 * −10: You gain 7 life, draw seven cards, then put up to seven permanent cards from your hand
 * onto the battlefield.
 * Loyalty: 7
 *
 * −X is Tezzeret the Seeker's `loyaltyAbilityX` reading the chosen X through
 * `manaValueAtMostX()`, over Decree of Annihilation's per-permanent exile sweep, narrowed to
 * colored permanents ([CardPredicate.IsColored], projected colors). The −10 ends with The World
 * Spell's [Patterns.Hand.putFromHand] "up to N" selection.
 */
val UginTheSpiritDragon = card("Ugin, the Spirit Dragon") {
    manaCost = "{8}"
    colorIdentity = ""
    typeLine = "Legendary Planeswalker — Ugin"
    startingLoyalty = 7
    oracleText = "+2: Ugin deals 3 damage to any target.\n" +
        "−X: Exile each permanent with mana value X or less that's one or more colors.\n" +
        "−10: You gain 7 life, draw seven cards, then put up to seven permanent cards from your " +
        "hand onto the battlefield."

    loyaltyAbility(+2) {
        val anyTarget = target(Targets.Any)
        effect = Effects.DealDamage(3, anyTarget)
    }

    loyaltyAbilityX {
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(
                GameObjectFilter.Permanent.manaValueAtMostX().withCardPredicate(CardPredicate.IsColored)
            ),
            effect = Effects.Move(EffectTarget.IterationEntity, Zone.EXILE)
        )
        description = "Exile each permanent with mana value X or less that's one or more colors."
    }

    loyaltyAbility(-10) {
        effect = Effects.GainLife(7) then
            Effects.DrawCards(7) then
            Patterns.Hand.putFromHand(
                filter = GameObjectFilter.Permanent,
                count = 7,
                prompt = "Put up to seven permanent cards from your hand onto the battlefield",
            )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58c1e824-c8a9-4312-8e4c-a29a26d189a4.jpg?1783938716"
        ruling("2020-06-23", "If a permanent has {X} in its mana cost, X is considered to be 0.")
        ruling("2020-06-23", "The mana value of a token that isn't a copy of another object is 0. A token that is a copy of another object has the same mana cost as that object.")
        ruling("2014-11-24", "If you choose to put permanent cards with morph onto the battlefield with the third ability, you must put them onto the battlefield face up.")
    }
}
