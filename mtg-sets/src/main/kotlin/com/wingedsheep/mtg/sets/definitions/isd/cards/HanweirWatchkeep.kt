package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

private val HanweirWatchkeepFront = card("Hanweir Watchkeep") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior Werewolf"
    power = 1
    toughness = 5
    oracleText = "Defender\nAt the beginning of each upkeep, if no spells were cast last turn, transform this creature."

    keywords(Keyword.DEFENDER)
    triggeredAbility {
        trigger = Triggers.EachUpkeep
        triggerCondition = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastLastTurn(), ComparisonOperator.EQ, DynamicAmount.Fixed(0)
        )
        effect = TransformEffect(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "145"
        artist = "Wayne Reynolds"
        flavorText = "He scans for wolves, knowing there's one he can never anticipate."
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b14ed17-1a35-4c49-ac46-3cad42d46c14.jpg?1783940944"
    }
}

private val BaneOfHanweir = card("Bane of Hanweir") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R"
    typeLine = "Creature — Werewolf"
    power = 5
    toughness = 5
    oracleText = "This creature attacks each combat if able.\nAt the beginning of each upkeep, if a player cast two or more spells last turn, transform this creature."

    staticAbility { ability = MustAttack() }
    triggeredAbility {
        trigger = Triggers.EachUpkeep
        triggerCondition = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastLastTurn(), ComparisonOperator.GTE, DynamicAmount.Fixed(2)
        )
        effect = TransformEffect(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "145"
        artist = "Wayne Reynolds"
        flavorText = "Technically he never left his post. He looks after the wolf wherever it goes."
        imageUri = "https://cards.scryfall.io/normal/back/2/b/2b14ed17-1a35-4c49-ac46-3cad42d46c14.jpg?1783940944"
    }
}

val HanweirWatchkeep: CardDefinition = CardDefinition.doubleFacedCreature(HanweirWatchkeepFront, BaneOfHanweir)
