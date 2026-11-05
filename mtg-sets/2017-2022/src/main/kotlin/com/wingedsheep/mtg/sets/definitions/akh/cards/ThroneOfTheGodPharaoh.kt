package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Throne of the God-Pharaoh
 * {2}
 * Legendary Artifact
 * At the beginning of your end step, each opponent loses life equal to the number of tapped
 * creatures you control.
 */
val ThroneOfTheGodPharaoh = card("Throne of the God-Pharaoh") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Legendary Artifact"
    oracleText = "At the beginning of your end step, each opponent loses life equal to the number of " +
        "tapped creatures you control."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.LoseLife(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature.tapped()).count(),
            EffectTarget.PlayerRef(Player.EachOpponent)
        )
        description = "At the beginning of your end step, each opponent loses life equal to the number " +
            "of tapped creatures you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "237"
        artist = "Titus Lunter"
        flavorText = "\"When the Second Sun rests between the horns on the horizon, so begins the Hour of " +
            "Revelation. Then the Hour of Glory, the Hour of Promise, and finally the Hour of Eternity.\"\n" +
            "—*The Accounting of Hours*"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe402b8e-f966-4971-90a5-950d8cff5025.jpg"
    }
}
