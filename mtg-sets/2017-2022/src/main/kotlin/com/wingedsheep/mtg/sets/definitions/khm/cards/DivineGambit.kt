package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Divine Gambit — Kaldheim #8
 * {W}{W} · Sorcery
 *
 * Exile target artifact, creature, or enchantment an opponent controls. That player may put a
 * permanent card from their hand onto the battlefield.
 *
 * "That player" is [Player.ControllerOf] the exiled permanent, read from last-known information
 * once it has left the battlefield (CR 608.2h), the same shape as Path to Exile. The put runs under
 * [Effects.ForEachPlayer], which rebinds `Player.You` (and the chooser) to that player, so *they*
 * pick from *their* hand and the card enters under their control. `putFromHand`'s up-to-one
 * selection is the "may". An illegal target fizzles the whole spell, so no one puts anything.
 */
val DivineGambit = card("Divine Gambit") {
    manaCost = "{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Exile target artifact, creature, or enchantment an opponent controls. That player may " +
        "put a permanent card from their hand onto the battlefield."

    spell {
        val permanent = target(
            TargetFilter(GameObjectFilter.ArtifactCreatureOrEnchantment.opponentControls())
        )
        effect = Effects.Exile(permanent) then
            Effects.ForEachPlayer(
                Player.ControllerOf("target artifact, creature, or enchantment"),
                listOf(
                    Patterns.Hand.putFromHand(
                        filter = GameObjectFilter.Permanent,
                        prompt = "You may put a permanent card from your hand onto the battlefield"
                    )
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "8"
        artist = "Joe Slucher"
        flavorText = "There are rules, and then there are gods."
        imageUri = "https://cards.scryfall.io/normal/front/6/9/696a8c12-4a1f-4b96-a921-538fa1a2de43.jpg?1783928285"
        ruling(
            "2021-02-05",
            "If the artifact, creature, or enchantment is an illegal target as Divine Gambit tries to " +
                "resolve, Divine Gambit won't resolve and none of its effects will happen. No player will " +
                "put any cards onto the battlefield."
        )
        ruling(
            "2021-02-05",
            "If the player chooses to put a double-faced card onto the battlefield, it enters with its " +
                "front face up. They may do this only if the front face of the card is a permanent card."
        )
    }
}
