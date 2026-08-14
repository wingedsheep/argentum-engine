package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Duskwatch Recruiter // Krallenhorde Howler (Shadows over Innistrad — the card's earliest
 * printing; also reprinted in Shadows over Innistrad Remastered and Innistrad Remastered)
 *
 * Front — Duskwatch Recruiter ({1}{G}, Creature — Human Warrior Werewolf, 2/2)
 *   {2}{G}: Look at the top three cards of your library. You may reveal a creature card from
 *   among them and put it into your hand. Put the rest on the bottom of your library in any order.
 *   At the beginning of each upkeep, if no spells were cast last turn, transform this creature.
 *
 * Back — Krallenhorde Howler (Creature — Werewolf, 3/3)
 *   Creature spells you cast cost {1} less to cast.
 *   At the beginning of each upkeep, if a player cast two or more spells last turn, transform
 *   this creature.
 *
 * Implementation:
 *  - The dig is [Patterns.Library.lookAtTopRevealMatchingToHand] (count 3, creature filter, the
 *    reveal-to-hand is `ChooseUpTo(1)` so declining is legal), rest to the bottom. Current Oracle
 *    reads "in any order" — the printed SOI wording was "in a random order" — so the remainder
 *    uses [CardOrder.ControllerChooses].
 *  - Both upkeep flips are the standard Werewolf pair: [Triggers.EachUpkeep] with an
 *    intervening-if on [DynamicAmounts.spellsCastLastTurn] (== 0 front, >= 2 back).
 *  - The back's cost reduction is [ModifySpellCost] over [SpellCostTarget.YouCast]; it applies to
 *    every creature spell its controller casts, not just Werewolves.
 */

private val DuskwatchRecruiterFront = card("Duskwatch Recruiter") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Warrior Werewolf"
    power = 2
    toughness = 2
    oracleText = "{2}{G}: Look at the top three cards of your library. You may reveal a creature " +
        "card from among them and put it into your hand. Put the rest on the bottom of your " +
        "library in any order.\n" +
        "At the beginning of each upkeep, if no spells were cast last turn, transform this creature."

    activatedAbility {
        cost = Costs.Mana("{2}{G}")
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = DynamicAmount.Fixed(3),
            filter = GameObjectFilter.Creature,
            prompt = "You may reveal a creature card and put it into your hand",
            restOrder = CardOrder.ControllerChooses,
        )
        description = "{2}{G}: Look at the top three cards of your library. You may reveal a " +
            "creature card from among them and put it into your hand. Put the rest on the " +
            "bottom of your library in any order."
    }

    triggeredAbility {
        trigger = Triggers.EachUpkeep
        triggerCondition = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastLastTurn(), ComparisonOperator.EQ, DynamicAmount.Fixed(0)
        )
        effect = TransformEffect(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "203"
        artist = "Craig J Spearing"
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e1915d93-c7dd-4bb7-bd5c-63a359a02b97.jpg?1783937740"
    }
}

private val KrallenhordeHowler = card("Krallenhorde Howler") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G"
    typeLine = "Creature — Werewolf"
    power = 3
    toughness = 3
    oracleText = "Creature spells you cast cost {1} less to cast.\n" +
        "At the beginning of each upkeep, if a player cast two or more spells last turn, " +
        "transform this creature."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Creature),
            modification = CostModification.ReduceGeneric(1),
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
        rarity = Rarity.UNCOMMON
        collectorNumber = "203"
        artist = "Craig J Spearing"
        imageUri = "https://cards.scryfall.io/normal/back/e/1/e1915d93-c7dd-4bb7-bd5c-63a359a02b97.jpg?1783937740"
    }
}

val DuskwatchRecruiter: CardDefinition =
    CardDefinition.doubleFacedCreature(DuskwatchRecruiterFront, KrallenhordeHowler)
