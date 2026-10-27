package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Codespell Cleric
 * {W}
 * Creature — Human Cleric
 * 1/1
 * Vigilance
 * When this creature enters, if it was the second spell you cast this turn, put a +1/+1 counter on
 * target creature.
 *
 * "It was the second spell you cast this turn" is the Cleric's position in your cast history, not a
 * resolution-time total: exactly one of your casts is recorded *before* its own cast record
 * (`beforeTriggeringSpell`, keyed off the entering Cleric, which keeps its entity id from the stack).
 * So a spell cast in response to the Cleric — or to its trigger — doesn't move it out of second
 * place, and a Cleric put onto the battlefield without being cast has no record and never triggers.
 * It is an intervening "if": checked as it enters and again on resolution.
 */
val CodespellCleric = card("Codespell Cleric") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    oracleText = "Vigilance\n" +
        "When this creature enters, if it was the second spell you cast this turn, " +
        "put a +1/+1 counter on target creature."
    power = 1
    toughness = 1

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastThisTurn(beforeTriggeringSpell = true),
            ComparisonOperator.EQ,
            1,
        )
        val recipient = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, recipient)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "7"
        artist = "Manuel Castañón"
        flavorText = "\"Today we are not Beskir or Tuskeri. We must fight as one clan, shoulder to " +
            "shoulder, to defend our rightful home!\""
        imageUri = "https://cards.scryfall.io/normal/front/1/3/1316a74e-da55-4668-87f2-98389a90a2a4.jpg?1783928286"
    }
}
