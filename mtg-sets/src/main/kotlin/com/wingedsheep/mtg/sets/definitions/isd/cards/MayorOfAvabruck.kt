package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

private val MayorOfAvabruckFront = card("Mayor of Avabruck") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Advisor Werewolf"
    power = 1
    toughness = 1
    oracleText = "Other Human creatures you control get +1/+1.\n" +
        "At the beginning of each upkeep, if no spells were cast last turn, transform this creature."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withSubtype(Subtype.HUMAN).youControl(),
                excludeSelf = true,
            ),
        )
    }
    triggeredAbility {
        trigger = Triggers.EachUpkeep
        triggerCondition = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastLastTurn(),
            ComparisonOperator.EQ,
            DynamicAmount.Fixed(0),
        )
        effect = TransformEffect(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "193"
        artist = "Svetlin Velinov"
        flavorText = "He can deny his true nature for only so long."
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd8ca448-f734-4cb9-b1d5-790eed9a4b2d.jpg?1783940920"
    }
}

private val HowlpackAlpha = card("Howlpack Alpha") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G"
    typeLine = "Creature — Werewolf"
    power = 3
    toughness = 3
    oracleText = "Each other creature you control that's a Werewolf or a Wolf gets +1/+1.\n" +
        "At the beginning of your end step, create a 2/2 green Wolf creature token.\n" +
        "At the beginning of each upkeep, if a player cast two or more spells last turn, transform this creature."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature
                    .withAnyOfSubtypes(listOf(Subtype.WEREWOLF, Subtype.WOLF))
                    .youControl(),
                excludeSelf = true,
            ),
        )
    }
    triggeredAbility {
        trigger = Triggers.YourEndStep
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Wolf"),
            imageUri = "https://cards.scryfall.io/normal/front/a/5/a53f8031-aaa8-424c-929a-5478538a8cc6.jpg?1783940880",
        )
    }
    triggeredAbility {
        trigger = Triggers.EachUpkeep
        triggerCondition = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastLastTurn(),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(2),
        )
        effect = TransformEffect(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "193"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/back/d/d/dd8ca448-f734-4cb9-b1d5-790eed9a4b2d.jpg?1783940920"
        ruling(
            "2025-01-24",
            "A creature that is both a Werewolf and a Wolf gets only +1/+1 from Howlpack Alpha's first ability.",
        )
    }
}

val MayorOfAvabruck: CardDefinition = CardDefinition.doubleFacedCreature(MayorOfAvabruckFront, HowlpackAlpha)
