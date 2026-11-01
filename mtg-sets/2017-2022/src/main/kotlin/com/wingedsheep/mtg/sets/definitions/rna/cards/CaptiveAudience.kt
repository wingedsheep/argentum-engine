package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private const val ZOMBIE_TOKEN_IMAGE =
    "https://cards.scryfall.io/normal/front/2/7/27b69e52-f394-4863-943b-239e96b5cc95.jpg?1783933604"

/**
 * Captive Audience
 * {5}{B}{R}
 * Enchantment
 * This enchantment enters under the control of an opponent of your choice.
 * At the beginning of your upkeep, choose one that hasn't been chosen —
 * • Your life total becomes 4.
 * • Discard your hand.
 * • Each opponent creates five 2/2 black Zombie creature tokens.
 *
 * "Your" is its controller's — the opponent it entered under — so that player loses the upkeeps
 * and makes the choices; the Zombies go to that player's opponents. Once all three modes have been
 * chosen the trigger has no modes left and does nothing.
 */
val CaptiveAudience = card("Captive Audience") {
    manaCost = "{5}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Enchantment"
    oracleText = "This enchantment enters under the control of an opponent of your choice.\n" +
        "At the beginning of your upkeep, choose one that hasn't been chosen —\n" +
        "• Your life total becomes 4.\n" +
        "• Discard your hand.\n" +
        "• Each opponent creates five 2/2 black Zombie creature tokens."

    replacementEffect(EntersWithChoice(ChoiceType.OPPONENT, chosenOpponentControls = true))

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = ModalEffect.chooseOneNotYetChosen(
            Mode.noTarget(Effects.SetLifeTotal(4), "Your life total becomes 4"),
            Mode.noTarget(Patterns.Hand.discardHand(), "Discard your hand"),
            Mode.noTarget(
                Effects.CreateToken(
                    power = 2,
                    toughness = 2,
                    colors = setOf(Color.BLACK),
                    creatureTypes = setOf("Zombie"),
                    count = 5,
                    controller = EffectTarget.PlayerRef(Player.EachOpponent),
                    imageUri = ZOMBIE_TOKEN_IMAGE,
                ),
                "Each opponent creates five 2/2 black Zombie creature tokens",
            ),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "160"
        artist = "Dmitry Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/0/6/065f63b2-472e-4148-8294-88ed38a5685f.jpg?1783933656"
    }
}
