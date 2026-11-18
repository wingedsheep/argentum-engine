package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Goblin Kaboomist
 * {1}{R}
 * Creature — Goblin Warrior
 * 1/2
 * At the beginning of your upkeep, create a colorless artifact token named Land Mine with "{R},
 * Sacrifice this token: This token deals 2 damage to target attacking creature without flying."
 * Then flip a coin. If you lose the flip, this creature deals 2 damage to itself.
 *
 * The Land Mine is the shared predefined token (`PredefinedTokens.LandMine`); it is created before
 * the flip and regardless of its result.
 */
val GoblinKaboomist = card("Goblin Kaboomist") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Warrior"
    oracleText = "At the beginning of your upkeep, create a colorless artifact token named Land Mine with " +
        "\"{R}, Sacrifice this token: This token deals 2 damage to target attacking creature without flying.\" " +
        "Then flip a coin. If you lose the flip, this creature deals 2 damage to itself."
    power = 1
    toughness = 2

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.CreatePredefinedToken("Land Mine") then
            Effects.FlipCoin(
                wonEffect = null,
                lostEffect = Effects.DealDamage(2, EffectTarget.Self),
            )
        description = "At the beginning of your upkeep, create a Land Mine token. Then flip a coin. " +
            "If you lose the flip, this creature deals 2 damage to itself."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "144"
        artist = "Kev Walker"
        flavorText = "Designed by Stone Librande"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80e1d85a-18e5-4c6a-85fd-a009bd8fda38.jpg?1783939173"
        ruling(
            "2014-07-18",
            "You flip the coin after the Land Mine token is created, not after you sacrifice a Land Mine. " +
                "The token is created even if you lose the flip.",
        )
        ruling(
            "2014-07-18",
            "The Land Mine tokens have the ability even if Goblin Kaboomist blows himself up or otherwise " +
                "leaves the battlefield.",
        )
    }
}
