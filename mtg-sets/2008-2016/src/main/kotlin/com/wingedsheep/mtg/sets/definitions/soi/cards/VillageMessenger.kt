package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step

private val VillageMessengerFront = card("Village Messenger") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Werewolf"
    power = 1
    toughness = 1
    oracleText = "Haste\nAt the beginning of each upkeep, if no spells were cast last turn, transform this creature."

    keywords(Keyword.HASTE)
    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastLastTurn(), ComparisonOperator.EQ, 0
        )
        effect = Effects.Transform(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "190"
        artist = "Daarken"
        flavorText = "\"I'm afraid it's bad news.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/4/44deb8cf-b9d1-4a14-aeaf-e62cf3dc9ef2.jpg?1783937747"
    }
}

private val MoonriseIntruder = card("Moonrise Intruder") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R"
    typeLine = "Creature — Werewolf"
    power = 2
    toughness = 2
    oracleText = "Menace\nAt the beginning of each upkeep, if a player cast two or more spells last turn, transform this creature."

    keywords(Keyword.MENACE)
    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.mostSpellsCastLastTurnByAPlayer(), ComparisonOperator.GTE, 2
        )
        effect = Effects.Transform(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "190"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/back/4/4/44deb8cf-b9d1-4a14-aeaf-e62cf3dc9ef2.jpg?1783937747"
    }
}

val VillageMessenger: CardDefinition = CardDefinition.doubleFacedCreature(VillageMessengerFront, MoonriseIntruder)
