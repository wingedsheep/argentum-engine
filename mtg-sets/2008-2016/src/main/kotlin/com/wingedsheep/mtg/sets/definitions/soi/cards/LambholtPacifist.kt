package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lambholt Pacifist // Lambholt Butcher
 *
 * "A creature" (not "another") — [Conditions.YouControl] without `excludeSelf`, so the Pacifist
 * satisfies its own restriction once its power is 4 or greater (ruling).
 */
private val LambholtPacifistFront = card("Lambholt Pacifist") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Shaman Werewolf"
    power = 3
    toughness = 3
    oracleText = "This creature can't attack unless you control a creature with power 4 or greater.\n" +
        "At the beginning of each upkeep, if no spells were cast last turn, transform this creature."

    staticAbility {
        ability = CantAttackUnless(
            Conditions.YouControl(GameObjectFilter.Creature.powerAtLeast(4))
        )
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
        collectorNumber = "215"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/23f3fa96-6276-463e-8033-a64c8f06c933.jpg?1783937732"
        ruling(
            "2016-04-08",
            "If Lambholt Pacifist's power becomes 4 or greater, it fulfills its own restriction and can attack."
        )
    }
}

private val LambholtButcher = card("Lambholt Butcher") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G"
    typeLine = "Creature — Werewolf"
    power = 4
    toughness = 4
    oracleText = "At the beginning of each upkeep, if a player cast two or more spells last turn, transform this creature."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.mostSpellsCastLastTurnByAPlayer(), ComparisonOperator.GTE, 2
        )
        effect = Effects.Transform(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "215"
        artist = "Ryan Pancoast"
        flavorText = "True peace can't be achieved until everyone else is dead."
        imageUri = "https://cards.scryfall.io/normal/back/2/3/23f3fa96-6276-463e-8033-a64c8f06c933.jpg?1783937732"
    }
}

val LambholtPacifist: CardDefinition =
    CardDefinition.doubleFacedCreature(LambholtPacifistFront, LambholtButcher)
