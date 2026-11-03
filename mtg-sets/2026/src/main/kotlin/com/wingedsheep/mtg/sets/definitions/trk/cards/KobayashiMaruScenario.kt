package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Kobayashi Maru Scenario
 * {1}{U}{U}
 * Enchantment
 * At the beginning of your upkeep, choose one that hasn't been chosen —
 * • Hail — Return up to one target creature an opponent controls with mana value 3 or less to its
 *   owner's hand.
 * • Sensors — Draw two cards.
 * • Transport — Create two 1/1 red Officer creature tokens.
 * • Abandon Ship — You lose the game.
 *
 * The Demonic Pact shape: [ModalEffect.chooseOneNotYetChosen] remembers the modes this permanent
 * has chosen. "Up to one target" makes Hail choosable even with no legal creature to bounce (the
 * Sol'Kanar the Tainted optional-target mode), and once the first three are spent Abandon Ship is
 * the only pick left.
 */
val KobayashiMaruScenario = card("Kobayashi Maru Scenario") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, choose one that hasn't been chosen —\n" +
        "• Hail — Return up to one target creature an opponent controls with mana value 3 or less to its owner's hand.\n" +
        "• Sensors — Draw two cards.\n" +
        "• Transport — Create two 1/1 red Officer creature tokens.\n" +
        "• Abandon Ship — You lose the game."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = ModalEffect.chooseOneNotYetChosen(
            // • Hail — Return up to one target creature an opponent controls with mana value 3 or less to its owner's hand.
            mode("Hail — Return up to one target creature an opponent controls with mana value 3 or less to its owner's hand") {
                val creature = target(TargetFilter.CreatureOpponentControls.manaValueAtMost(3), optional = true)
                effect = Effects.ReturnToHand(creature)
            },
            // • Sensors — Draw two cards.
            Mode.noTarget(Effects.DrawCards(2), "Sensors — Draw two cards"),
            // • Transport — Create two 1/1 red Officer creature tokens.
            Mode.noTarget(
                Effects.CreateToken(
                    power = 1, toughness = 1,
                    colors = setOf(Color.RED),
                    creatureTypes = setOf("Officer"),
                    count = 2,
                    imageUri = "https://cards.scryfall.io/normal/front/d/6/d676cbef-8e31-4ae6-8dfe-6b84160f71e0.jpg?1793363893"
                ),
                "Transport — Create two 1/1 red Officer creature tokens"
            ),
            // • Abandon Ship — You lose the game.
            Mode.noTarget(Effects.LoseGame(), "Abandon Ship — You lose the game")
        )
        description = "At the beginning of your upkeep, choose one that hasn't been chosen —"
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "68"
        artist = "Kevin Glint"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7fb72cc-1bc6-4774-9798-44824de46c72.jpg?1792508172"
    }
}
