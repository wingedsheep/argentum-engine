package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CrewSaddleCharacteristic
import com.wingedsheep.sdk.scripting.CrewSaddleContribution
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Interface Ace — Aetherdrift #17
 * {1}{W} · Artifact Creature — Robot Pilot · 0/4
 *
 * The crew/saddle contribution reads projected toughness, so counters and continuous effects are
 * reflected without changing Interface Ace's actual power. The tap trigger is gated to its
 * controller's turn and capped independently per turn.
 */
val InterfaceAce = card("Interface Ace") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Robot Pilot"
    oracleText = "This creature saddles Mounts and crews Vehicles using its toughness rather than its power.\n" +
        "Whenever this creature becomes tapped during your turn, untap it. This ability triggers only once each turn."
    power = 0
    toughness = 4

    staticAbility {
        ability = CrewSaddleContribution(characteristic = CrewSaddleCharacteristic.TOUGHNESS)
    }

    triggeredAbility {
        trigger = Triggers.BecomesTapped
        triggerCondition = Conditions.IsYourTurn
        oncePerTurn = true
        effect = Effects.Untap(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "17"
        artist = "Wonchun Choi"
        flavorText = "To the Guidelight Voyagers, a vehicle is just another hardware upgrade."
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fcfd487a-a9e6-44e3-80af-bc384316106f.jpg?1783907918"
    }
}
