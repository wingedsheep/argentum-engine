package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step

private val createHuntmasterWolf = Effects.CreateToken(
    power = 2,
    toughness = 2,
    colors = setOf(Color.GREEN),
    creatureTypes = setOf("Wolf"),
    imageUri = "https://cards.scryfall.io/normal/front/8/9/89b89a55-3ea2-4186-b946-06831bc16169.jpg?1783907965",
)

private val huntmasterFrontTrigger = createHuntmasterWolf then Effects.GainLife(2)

private val HuntmasterOfTheFellsFront = card("Huntmaster of the Fells") {
    manaCost = "{2}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Human Werewolf"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature enters or transforms into Huntmaster of the Fells, create a 2/2 green Wolf creature token and you gain 2 life.\n" +
        "At the beginning of each upkeep, if no spells were cast last turn, transform this creature."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = huntmasterFrontTrigger
    }
    triggeredAbility {
        trigger = Triggers.self.transforms(false)
        effect = huntmasterFrontTrigger
    }
    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastLastTurn(),
            ComparisonOperator.EQ,
            0,
        )
        effect = Effects.Transform(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "140"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aae6fb12-b252-453b-bca7-1ea2a0d6c8dc.jpg?1783940803"
    }
}

private val RavagerOfTheFells = card("Ravager of the Fells") {
    manaCost = ""
    colorIdentity = "RG"
    colorIndicator = "RG"
    typeLine = "Creature — Werewolf"
    power = 4
    toughness = 4
    oracleText = "Trample\n" +
        "Whenever this creature transforms into Ravager of the Fells, it deals 2 damage to target opponent or planeswalker and 2 damage to up to one target creature that player or that planeswalker's controller controls.\n" +
        "At the beginning of each upkeep, if a player cast two or more spells last turn, transform this creature."

    keywords(Keyword.TRAMPLE)
    triggeredAbility {
        trigger = Triggers.self.transforms(true)
        val playerOrPlaneswalker = target(Targets.OpponentOrPlaneswalker)
        // "That player or that planeswalker's controller": the first branch reads a player
        // target, the second the controller of a planeswalker target (as on Chandra Nalaar).
        val creature = target(
            TargetFilter(
                GameObjectFilter.Creature.targetPlayerControls(playerOrPlaneswalker) or
                    GameObjectFilter.Creature.targetPlayerControls(EffectTarget.TargetController),
            ),
            optional = true,
        )
        effect = Effects.DealDamage(2, playerOrPlaneswalker) then Effects.DealDamage(2, creature)
    }
    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.mostSpellsCastLastTurnByAPlayer(),
            ComparisonOperator.GTE,
            2,
        )
        effect = Effects.Transform(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "140"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/back/a/a/aae6fb12-b252-453b-bca7-1ea2a0d6c8dc.jpg?1783940803"
    }
}

val HuntmasterOfTheFells: CardDefinition =
    CardDefinition.doubleFacedCreature(HuntmasterOfTheFellsFront, RavagerOfTheFells)
