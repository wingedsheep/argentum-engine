package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/** Conduit of Storms // Conduit of Emrakul (Eldritch Moon). */
private val ConduitOfStormsFront = card("Conduit of Storms") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Werewolf Horror"
    oracleText = "Whenever this creature attacks, add {R} at the beginning of your next main phase this turn.\n{3}{R}{R}: Transform this creature."
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = CreateDelayedTriggerEffect(
            step = Step.POSTCOMBAT_MAIN,
            effect = Effects.AddMana(Color.RED),
            fireOnPlayer = EffectTarget.PlayerRef(Player.You),
        )
    }

    activatedAbility {
        cost = Costs.Mana("{3}{R}{R}")
        effect = TransformEffect(EffectTarget.Self)
        description = "Transform this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "124"
        artist = "Raymond Swanland"
        flavorText = "It strikes with the fury of a tempest."
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f95145a-41a1-478e-bf8a-ea8838d6f9b1.jpg?1783937474"
        ruling("2025-01-24", "You'll add mana at the beginning of the next main phase whether or not Conduit of Storms (or Conduit of Emrakul) is still on the battlefield.")
        ruling("2025-01-24", "If Conduit of Storms attacks and you transform it into Conduit of Emrakul in response, you'll add {R}, not {C}{C} and not {C}{C}{R}.")
    }
}

private val ConduitOfEmrakul = card("Conduit of Emrakul") {
    manaCost = ""
    colorIdentity = "R"
    typeLine = "Creature — Eldrazi Werewolf"
    oracleText = "Whenever this creature attacks, add {C}{C} at the beginning of your next main phase this turn."
    power = 5
    toughness = 4

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = CreateDelayedTriggerEffect(
            step = Step.POSTCOMBAT_MAIN,
            effect = Effects.AddColorlessMana(2),
            fireOnPlayer = EffectTarget.PlayerRef(Player.You),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "124"
        artist = "Raymond Swanland"
        flavorText = "As without, so within."
        imageUri = "https://cards.scryfall.io/normal/back/7/f/7f95145a-41a1-478e-bf8a-ea8838d6f9b1.jpg?1783937474"
    }
}

val ConduitOfStorms: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = ConduitOfStormsFront,
    backFace = ConduitOfEmrakul,
)
