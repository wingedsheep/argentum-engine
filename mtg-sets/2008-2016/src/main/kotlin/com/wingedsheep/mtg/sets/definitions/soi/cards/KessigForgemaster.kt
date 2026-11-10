package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private const val BLOCK_RULING = "triggers once for each creature blocking or blocked by it. The ability " +
    "resolves and deals damage to that creature before combat damage is dealt."

/**
 * "Blocks or becomes blocked by **a** creature" fires once per combat partner (Ornery Goblin's shape):
 * the explicit filter turns `oncePerCombat` off and the partner is the triggering entity.
 */
private val KessigForgemasterFront = card("Kessig Forgemaster") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Shaman Werewolf"
    power = 2
    toughness = 1
    oracleText = "Whenever this creature blocks or becomes blocked by a creature, this creature deals 1 damage " +
        "to that creature.\n" +
        "At the beginning of each upkeep, if no spells were cast last turn, transform this creature."

    triggeredAbility {
        trigger = Triggers.self.blocksOrBecomesBlocked(GameObjectFilter.Creature)
        effect = Effects.DealDamage(1, EffectTarget.TriggeringEntity)
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
        collectorNumber = "169"
        artist = "Winona Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33e542ce-1106-489d-9563-bd62886b8ed3.jpg?1783937754"
        ruling(
            "2016-04-08",
            "Kessig Forgemaster's first ability $BLOCK_RULING If that damage destroys a creature blocking " +
                "Kessig Forgemaster, Kessig Forgemaster doesn't become unblocked.",
        )
        ruling(
            "2016-04-08",
            "The abilities that transform a Werewolf back and forth look at the entire previous turn, even if " +
                "the Werewolf with that ability wasn't on the battlefield for some or all of that turn.",
        )
    }
}

private val FlameheartWerewolf = card("Flameheart Werewolf") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R"
    typeLine = "Creature — Werewolf"
    power = 3
    toughness = 2
    oracleText = "Whenever this creature blocks or becomes blocked by a creature, this creature deals 2 damage " +
        "to that creature.\n" +
        "At the beginning of each upkeep, if a player cast two or more spells last turn, transform this creature."

    triggeredAbility {
        trigger = Triggers.self.blocksOrBecomesBlocked(GameObjectFilter.Creature)
        effect = Effects.DealDamage(2, EffectTarget.TriggeringEntity)
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
        collectorNumber = "169"
        artist = "Winona Nelson"
        imageUri = "https://cards.scryfall.io/normal/back/3/3/33e542ce-1106-489d-9563-bd62886b8ed3.jpg?1783937754"
        ruling(
            "2016-04-08",
            "Flameheart Werewolf's first ability $BLOCK_RULING If that damage destroys a creature blocking " +
                "Flameheart Werewolf, Flameheart Werewolf doesn't become unblocked.",
        )
        ruling(
            "2016-04-08",
            "To trigger the Werewolf's back face's transform ability, a single player must have cast two or " +
                "more spells during the previous turn. If multiple players each cast just one spell during the " +
                "previous turn, the ability won't trigger.",
        )
    }
}

val KessigForgemaster: CardDefinition = CardDefinition.doubleFacedCreature(KessigForgemasterFront, FlameheartWerewolf)
