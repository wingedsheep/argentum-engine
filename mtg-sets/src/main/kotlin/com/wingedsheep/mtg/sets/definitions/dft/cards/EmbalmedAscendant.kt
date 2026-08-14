package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Embalmed Ascendant — Aetherdrift #201
 * {1}{W}{B} · Creature — Zombie · 1/2
 *
 * Start your engines!
 * When this creature enters, create a 2/2 black Zombie creature token.
 * Max speed — Whenever a creature you control dies, each opponent loses 1 life and you gain 1 life.
 *
 * The max-speed half is gated as a `triggerCondition` (CR 603.4), so it is checked both when a
 * creature dies and again on resolution — dropping below max speed in between correctly stops the
 * drain. [Triggers.YourCreatureDies] is an ANY binding over creatures you control, which includes
 * this creature itself: when it dies alongside another creature, both deaths see the ability.
 *
 * "Each opponent loses 1 life and you gain 1 life" is a two-part drain rather than life *lost* being
 * mirrored into life gained — you gain exactly 1 no matter how many opponents there are.
 */
val EmbalmedAscendant = card("Embalmed Ascendant") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Zombie"
    oracleText = "Start your engines!\n" +
        "When this creature enters, create a 2/2 black Zombie creature token.\n" +
        "Max speed — Whenever a creature you control dies, each opponent loses 1 life and you gain 1 life."
    power = 1
    toughness = 2

    startYourEngines()

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie")
        )
    }

    maxSpeed {
        triggeredAbility {
            trigger = Triggers.YourCreatureDies
            effect = Effects.Composite(
                Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)),
                Effects.GainLife(1)
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "201"
        artist = "Edgar Sánchez Hidalgo"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5cf181ae-daa7-42f9-b667-5e679d80cf34.jpg?1783907858"
        ruling(
            "2025-02-07",
            "Start your engines! isn't a triggered ability. Increasing your speed to 1 is something " +
                "that happens as a state-based action as soon as you control a permanent with the " +
                "ability. Notably, this includes gaining control of a permanent with the ability that " +
                "another player controls."
        )
        ruling(
            "2025-02-07",
            "“Max speed — [ability]” means “As long as you have max speed, this object has " +
                "[ability].” If the granted ability functions in a zone other than the battlefield, " +
                "the max speed ability does too."
        )
        ruling("2025-02-07", "A player “has max speed” if their speed is 4.")
    }
}
