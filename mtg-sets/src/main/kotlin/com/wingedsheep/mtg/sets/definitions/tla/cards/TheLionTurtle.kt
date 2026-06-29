package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.CantBlockUnless
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * The Lion-Turtle
 * {1}{G}{U}
 * Legendary Creature — Elder Cat Turtle
 * 3/6
 *
 * Vigilance, reach
 * When The Lion-Turtle enters, you gain 3 life.
 * The Lion-Turtle can't attack or block unless there are three or more Lesson cards in your graveyard.
 * {T}: Add one mana of any color.
 *
 * The "can't attack or block unless …" clause is two static restrictions ([CantAttackUnless] +
 * [CantBlockUnless]) sharing a single graveyard-count condition — composed inline from
 * [DynamicAmount.Count] over Lesson-subtyped cards in your graveyard rather than a bespoke
 * condition type.
 */
val TheLionTurtle = card("The Lion-Turtle") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Elder Cat Turtle"
    power = 3
    toughness = 6
    oracleText = "Vigilance, reach\n" +
        "When The Lion-Turtle enters, you gain 3 life.\n" +
        "The Lion-Turtle can't attack or block unless there are three or more Lesson cards in your graveyard.\n" +
        "{T}: Add one mana of any color."

    keywords(Keyword.VIGILANCE, Keyword.REACH)

    val threeOrMoreLessons = Conditions.CompareAmounts(
        DynamicAmount.Count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Any.withSubtype(Subtype.LESSON)),
        ComparisonOperator.GTE,
        DynamicAmount.Fixed(3)
    )

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.GainLife(3)
        description = "When The Lion-Turtle enters, you gain 3 life."
    }

    staticAbility {
        ability = CantAttackUnless(threeOrMoreLessons)
    }
    staticAbility {
        ability = CantBlockUnless(threeOrMoreLessons)
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "232"
        artist = "Yuumei"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/8491585c-81d0-48be-8ed0-832e42fad9c6.jpg?1764121714"
    }
}
