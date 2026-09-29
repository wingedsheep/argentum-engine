package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gnottvold Hermit // Chrome Host Hulk (March of the Machine #188)
 * {3}{G} Creature — Troll 4/4 // Creature — Phyrexian Troll 5/5 (green-blue color indicator)
 *
 * Front — "{5}{U/P}: Transform this creature. Activate only as a sorcery."
 * Back  — "Whenever this creature attacks, up to one other target creature has base power and
 *          toughness 5/5 until end of turn."
 */
private val GnottvoldHermitFront = card("Gnottvold Hermit") {
    manaCost = "{3}{G}"
    colorIdentity = "GU"
    typeLine = "Creature — Troll"
    power = 4
    toughness = 4
    oracleText = "{5}{U/P}: Transform this creature. Activate only as a sorcery. " +
        "({U/P} can be paid with either {U} or 2 life.)"

    activatedAbility {
        cost = Costs.Mana("{5}{U/P}")
        effect = Effects.Transform(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "188"
        artist = "Artur Nakhodkin"
        flavorText = "The Hagi trolls stood little chance against Phyrexia, having killed everyone who " +
            "came to Gnottvold to warn them."
        imageUri = "https://cards.scryfall.io/normal/front/7/0/70b2cdc5-35b9-443d-b499-c8b75c0d0a64.jpg?1783916974"
    }
}

private val ChromeHostHulk = card("Chrome Host Hulk") {
    manaCost = ""
    colorIndicator = "GU" // Transformed back face, no mana cost (CR 204).
    colorIdentity = "GU"
    typeLine = "Creature — Phyrexian Troll"
    power = 5
    toughness = 5
    oracleText = "Whenever this creature attacks, up to one other target creature has base power and " +
        "toughness 5/5 until end of turn."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(TargetFilter.Creature.other(), optional = true)
        effect = Effects.SetBasePowerAndToughness(5, 5, creature, Duration.EndOfTurn)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "188"
        artist = "Artur Nakhodkin"
        flavorText = "Now they saw the truth. Divisions between Hagi and Torga were meaningless. " +
            "There was only Phyrexia."
        imageUri = "https://cards.scryfall.io/normal/back/7/0/70b2cdc5-35b9-443d-b499-c8b75c0d0a64.jpg?1783916974"
    }
}

val GnottvoldHermit: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = GnottvoldHermitFront,
    backFace = ChromeHostHulk,
)
