package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import com.wingedsheep.sdk.scripting.values.DynamicAmount

private val AmbitiousFarmhandFront = card("Ambitious Farmhand") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Peasant"
    oracleText = "When this creature enters, you may search your library for a basic Plains card, " +
        "reveal it, put it into your hand, then shuffle.\n" +
        "Coven — {1}{W}{W}: Transform this creature. Activate only if you control three or more " +
        "creatures with different powers."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand.withSubtype(Subtype.PLAINS),
            destination = SearchDestination.HAND,
            reveal = true,
        )
    }

    activatedAbility {
        cost = Costs.Mana("{1}{W}{W}")
        effect = TransformEffect(EffectTarget.Self)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.CompareAmounts(
                    left = DynamicAmount.AggregateBattlefield(
                        player = Player.You,
                        filter = GameObjectFilter.Creature,
                        aggregation = Aggregation.DISTINCT_VALUES,
                        property = CardNumericProperty.POWER,
                    ),
                    operator = ComparisonOperator.GTE,
                    right = DynamicAmount.Fixed(3),
                ),
            ),
        )
        timing = TimingRule.SorcerySpeed
        description = "Coven — {1}{W}{W}: Transform this creature. Activate only if you control " +
            "three or more creatures with different powers."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "2"
        artist = "Bryan Sola"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/54d4e7c3-294d-4900-8b70-faafda17cc33.jpg?1783925674"
        ruling("2021-09-24", "For three creatures to have different powers from one another, each of their powers needs to be different.")
    }
}

private val SeasonedCathar = card("Seasoned Cathar") {
    manaCost = ""
    colorIdentity = "W"
    colorIndicator = "W"
    typeLine = "Creature — Human Knight"
    oracleText = "Lifelink"
    power = 3
    toughness = 3
    keywords(Keyword.LIFELINK)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "2"
        artist = "Bryan Sola"
        flavorText = "\"The cathars have taught me many things, but I will never forget the hard, honest work that first callused my hands.\""
        imageUri = "https://cards.scryfall.io/normal/back/5/4/54d4e7c3-294d-4900-8b70-faafda17cc33.jpg?1783925674"
    }
}

val AmbitiousFarmhand: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = AmbitiousFarmhandFront,
    backFace = SeasonedCathar,
)
