package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hermit of the Natterknolls // Lone Wolf of the Natterknolls
 *
 * "During your turn" restricts the cast event (checked when the spell is cast), not an
 * intervening "if" — hence [triggerRestriction] on the draw triggers.
 */
private val HermitOfTheNatterknollsFront = card("Hermit of the Natterknolls") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Werewolf"
    power = 2
    toughness = 3
    oracleText = "Whenever an opponent casts a spell during your turn, draw a card.\n" +
        "At the beginning of each upkeep, if no spells were cast last turn, transform this creature."

    triggeredAbility {
        trigger = Triggers.anOpponent.casts()
        triggerRestriction = Conditions.IsYourTurn
        effect = Effects.DrawCards(1)
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastLastTurn(), ComparisonOperator.EQ, 0
        )
        effect = Effects.Transform(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "209"
        artist = "Dan Murayama Scott"
        flavorText = "He hates company . . ."
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5e84d80-b5a6-4cd1-9f31-f6ca46bf2c0c.jpg?1783937736"
    }
}

private val LoneWolfOfTheNatterknolls = card("Lone Wolf of the Natterknolls") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G"
    typeLine = "Creature — Werewolf"
    power = 3
    toughness = 5
    oracleText = "Whenever an opponent casts a spell during your turn, draw two cards.\n" +
        "At the beginning of each upkeep, if a player cast two or more spells last turn, transform this creature."

    triggeredAbility {
        trigger = Triggers.anOpponent.casts()
        triggerRestriction = Conditions.IsYourTurn
        effect = Effects.DrawCards(2)
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.mostSpellsCastLastTurnByAPlayer(), ComparisonOperator.GTE, 2
        )
        effect = Effects.Transform(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "209"
        artist = "Dan Murayama Scott"
        flavorText = ". . . but he'll have you for lunch."
        imageUri = "https://cards.scryfall.io/normal/back/d/5/d5e84d80-b5a6-4cd1-9f31-f6ca46bf2c0c.jpg?1783937736"
    }
}

val HermitOfTheNatterknolls: CardDefinition =
    CardDefinition.doubleFacedCreature(HermitOfTheNatterknollsFront, LoneWolfOfTheNatterknolls)
