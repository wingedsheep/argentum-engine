package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pyretic Prankster // Glistening Goremonger — March of the Machine #157.
 * {1}{R} · Creature — Devil 2/1 // Creature — Phyrexian Devil 3/2
 *
 * Front: {3}{B/P}: Transform this creature. Activate only as a sorcery.
 * Back: When this creature dies, each opponent sacrifices an artifact or creature of their choice.
 */
private val PyreticPranksterFront = card("Pyretic Prankster") {
    manaCost = "{1}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Devil"
    power = 2
    toughness = 1
    oracleText = "{3}{B/P}: Transform this creature. Activate only as a sorcery. " +
        "({B/P} can be paid with either {B} or 2 life.)"

    activatedAbility {
        cost = Costs.Mana("{3}{B/P}")
        effect = Effects.Transform(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "157"
        artist = "Francis Tneh"
        flavorText = "It'd played the same prank a hundred times, never noticing the flame's slowly evolving pattern."
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0ee40c4b-2ca3-4cda-bc9e-451455c17adc.jpg?1783916990"
        ruling("2023-04-14", "If an opponent controls only artifacts and no creatures, or vice versa, they must sacrifice one. They can’t choose to sacrifice an artifact and then fail to do so while they control a creature.")
    }
}

private val GlisteningGoremonger = card("Glistening Goremonger") {
    manaCost = ""
    colorIdentity = "BR"
    colorIndicator = "BR"
    typeLine = "Creature — Phyrexian Devil"
    power = 3
    toughness = 2
    oracleText = "When this creature dies, each opponent sacrifices an artifact or creature of their choice."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.Sacrifice(
            GameObjectFilter.Artifact.or(GameObjectFilter.Creature),
            1,
            EffectTarget.PlayerRef(Player.EachOpponent)
        )
        description = "When this creature dies, each opponent sacrifices an artifact or creature of their choice."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "157"
        artist = "Francis Tneh"
        flavorText = "When it realized the symbol's true, glorious meaning, it began marking every surface on Innistrad with oil-soaked claws."
        imageUri = "https://cards.scryfall.io/normal/back/0/e/0ee40c4b-2ca3-4cda-bc9e-451455c17adc.jpg?1783916990"
    }
}

val PyreticPrankster: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = PyreticPranksterFront,
    backFace = GlisteningGoremonger,
)
