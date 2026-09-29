package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Herbology Instructor // Malady Invoker (March of the Machine #189)
 * {1}{G} Creature — Treefolk Druid 1/3 // Creature — Phyrexian Treefolk 3/3 (black-green color indicator)
 *
 * Front — "When this creature enters, you gain 3 life. {6}{B/P}: Transform this creature.
 *          Activate only as a sorcery."
 * Back  — "When this creature transforms into Malady Invoker, target creature an opponent controls
 *          gets -0/-X until end of turn, where X is this creature's power."
 */
private val HerbologyInstructorFront = card("Herbology Instructor") {
    manaCost = "{1}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Treefolk Druid"
    power = 1
    toughness = 3
    oracleText = "When this creature enters, you gain 3 life.\n{6}{B/P}: Transform this creature. " +
        "Activate only as a sorcery. ({B/P} can be paid with either {B} or 2 life.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(3)
    }

    activatedAbility {
        cost = Costs.Mana("{6}{B/P}")
        effect = Effects.Transform(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "189"
        artist = "Sergey Glushakov"
        flavorText = "\"As you see, these flowers . . . can have either restorative or poisonous . . . " +
            "what is that?\""
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40ab763d-05ee-408d-aeba-eaf18c4f2e21.jpg?1783916974"
    }
}

private val MaladyInvoker = card("Malady Invoker") {
    manaCost = ""
    colorIndicator = "BG" // Transformed back face, no mana cost (CR 204).
    colorIdentity = "BG"
    typeLine = "Creature — Phyrexian Treefolk"
    power = 3
    toughness = 3
    oracleText = "When this creature transforms into Malady Invoker, target creature an opponent " +
        "controls gets -0/-X until end of turn, where X is this creature's power."

    triggeredAbility {
        trigger = Triggers.self.transforms(true)
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.ModifyStats(DynamicAmounts.fixed(0), -DynamicAmounts.sourcePower(), creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "189"
        artist = "Sergey Glushakov"
        flavorText = "\"How dare you leave before my lesson is done!\""
        imageUri = "https://cards.scryfall.io/normal/back/4/0/40ab763d-05ee-408d-aeba-eaf18c4f2e21.jpg?1783916974"
    }
}

val HerbologyInstructor: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = HerbologyInstructorFront,
    backFace = MaladyInvoker,
)
