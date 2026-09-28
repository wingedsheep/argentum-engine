package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Blightreaper Thallid // Blightsower Thallid — March of the Machine #92.
 * {1}{B} · Creature — Fungus 2/2 // Creature — Phyrexian Fungus 3/3
 *
 * Front: {3}{G/P}: Transform this creature. Activate only as a sorcery.
 * Back: When this creature transforms into Blightsower Thallid or dies, create a 1/1 green
 * Phyrexian Saproling creature token.
 */
private val BlightreaperThallidFront = card("Blightreaper Thallid") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Fungus"
    power = 2
    toughness = 2
    oracleText = "{3}{G/P}: Transform this creature. Activate only as a sorcery. " +
        "({G/P} can be paid with either {G} or 2 life.)"

    activatedAbility {
        cost = Costs.Mana("{3}{G/P}")
        effect = Effects.Transform(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "92"
        artist = "Marta Nael"
        flavorText = "Mudcreep loved the taste of the funny oil that leaked from the wrecked machines in Urborg."
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1150ea9-02b5-4767-a529-6149d758830e.jpg?1783917024"
    }
}

private val BlightsowerThallid = card("Blightsower Thallid") {
    manaCost = ""
    colorIdentity = "BG"
    colorIndicator = "B"
    typeLine = "Creature — Phyrexian Fungus"
    power = 3
    toughness = 3
    oracleText = "When this creature transforms into Blightsower Thallid or dies, create a 1/1 green " +
        "Phyrexian Saproling creature token."

    triggeredAbility {
        trigger = Triggers.or(Triggers.self.transforms(true), Triggers.self.dies())
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Phyrexian", "Saproling"),
            imageUri = "https://cards.scryfall.io/normal/front/a/1/a10358f5-d653-49a0-9d81-a5d4e6dafe25.jpg?1783916669"
        )
        description = "When this creature transforms into Blightsower Thallid or dies, create a 1/1 green " +
            "Phyrexian Saproling creature token."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "92"
        artist = "Marta Nael"
        flavorText = "Then one day, Mudcreep started to feel funny itself."
        imageUri = "https://cards.scryfall.io/normal/back/f/1/f1150ea9-02b5-4767-a529-6149d758830e.jpg?1783917024"
    }
}

val BlightreaperThallid: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = BlightreaperThallidFront,
    backFace = BlightsowerThallid,
)
