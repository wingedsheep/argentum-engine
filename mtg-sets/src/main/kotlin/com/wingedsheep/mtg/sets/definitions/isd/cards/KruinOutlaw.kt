package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

private val KruinOutlawFront = card("Kruin Outlaw") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Rogue Werewolf"
    power = 2
    toughness = 2
    oracleText = "First strike\nAt the beginning of each upkeep, if no spells were cast last turn, transform this creature."

    keywords(Keyword.FIRST_STRIKE)
    triggeredAbility {
        trigger = Triggers.EachUpkeep
        triggerCondition = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastLastTurn(), ComparisonOperator.EQ, DynamicAmount.Fixed(0)
        )
        effect = TransformEffect(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "152"
        artist = "David Rapoza"
        flavorText = "\"Hold tight. I've got a surprise for them.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec00d2d2-6597-474a-9353-345bbedfe57e.jpg?1783940938"
        ruling(
            "2011-09-22",
            "If Kruin Outlaw somehow transforms after blockers have been declared but before combat ends, " +
                "any Werewolves you control that are blocked by a single creature will remain blocked."
        )
    }
}

private val TerrorOfKruinPass = card("Terror of Kruin Pass") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R"
    typeLine = "Creature — Werewolf"
    power = 3
    toughness = 3
    oracleText = "Double strike\nWerewolves you control have menace. (A creature with menace can't be blocked except by two or more creatures.)\nAt the beginning of each upkeep, if a player cast two or more spells last turn, transform this creature."

    keywords(Keyword.DOUBLE_STRIKE)
    staticAbility {
        ability = GrantKeyword(
            Keyword.MENACE,
            GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.WEREWOLF).youControl())
        )
    }
    triggeredAbility {
        trigger = Triggers.EachUpkeep
        triggerCondition = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastLastTurn(), ComparisonOperator.GTE, DynamicAmount.Fixed(2)
        )
        effect = TransformEffect(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "152"
        artist = "David Rapoza"
        imageUri = "https://cards.scryfall.io/normal/back/e/c/ec00d2d2-6597-474a-9353-345bbedfe57e.jpg?1783940938"
    }
}

val KruinOutlaw: CardDefinition = CardDefinition.doubleFacedCreature(KruinOutlawFront, TerrorOfKruinPass)
