package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Emeria's Call {4}{W}{W}{W} // Emeria, Shattered Skyclave — Zendikar Rising #12
 * Sorcery // Land · Modal double-faced card
 *
 * Front — Emeria's Call: "Create two 4/4 white Angel Warrior creature tokens with flying.
 * Non-Angel creatures you control gain indestructible until your next turn."
 * Back — Emeria, Shattered Skyclave: "As this land enters, you may pay 3 life. If you don't, it
 * enters tapped. {T}: Add {W}."
 *
 * The indestructible set is locked in as the spell resolves (ForEachInGroup, Selfless Spirit's
 * shape), so creatures you gain control of later don't get it; the new Angel tokens are excluded
 * by the non-Angel predicate anyway.
 */
private val EmeriasCallFront = card("Emeria's Call") {
    manaCost = "{4}{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Create two 4/4 white Angel Warrior creature tokens with flying. " +
        "Non-Angel creatures you control gain indestructible until your next turn."

    spell {
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Angel", "Warrior"),
            keywords = setOf(Keyword.FLYING),
            count = 2,
        ) then Effects.ForEachInGroup(
            GroupFilter(
                GameObjectFilter(
                    cardPredicates = listOf(
                        CardPredicate.IsCreature,
                        CardPredicate.NotSubtype(Subtype("Angel")),
                    )
                ).youControl()
            ),
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.IterationEntity, Duration.UntilYourNextTurn)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "12"
        artist = "Matt Stewart"
        flavorText = "\"Iona no longer guards this place. We do.\"\n—Kasla, Emeria shepherd"
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c470539a-9cc7-4175-8f7c-c982b6072b6d.jpg?1783929425"
    }
}

private val EmeriaShatteredSkyclaveBack = card("Emeria, Shattered Skyclave") {
    typeLine = "Land"
    colorIdentity = "W"
    oracleText = "As this land enters, you may pay 3 life. If you don't, it enters tapped.\n{T}: Add {W}."

    replacementEffect(EntersTapped(payLifeCost = 3))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "12"
        artist = "Matt Stewart"
        flavorText = "\"You called it the castle of a god. It is less than that, and so much more.\"\n—Kasla, Emeria shepherd"
        imageUri = "https://cards.scryfall.io/normal/back/c/4/c470539a-9cc7-4175-8f7c-c982b6072b6d.jpg?1783929425"
    }
}

val EmeriasCall: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = EmeriasCallFront,
    backFace = EmeriaShatteredSkyclaveBack,
)
