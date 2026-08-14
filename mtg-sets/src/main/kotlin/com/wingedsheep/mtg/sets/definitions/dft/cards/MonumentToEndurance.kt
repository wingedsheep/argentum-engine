package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Monument to Endurance — Aetherdrift #237
 * {3} · Artifact
 *
 * Whenever you discard a card, choose one that hasn't been chosen this turn —
 * • Draw a card.
 * • Create a Treasure token.
 * • Each opponent loses 3 life.
 *
 * The turn-scoped modal primitive records choices per Monument. Once all three modes have been
 * chosen in a turn, later discard triggers resolve without an effect, matching the card's ruling.
 */
val MonumentToEndurance = card("Monument to Endurance") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Whenever you discard a card, choose one that hasn't been chosen this turn —\n" +
        "• Draw a card.\n" +
        "• Create a Treasure token.\n" +
        "• Each opponent loses 3 life."

    triggeredAbility {
        trigger = Triggers.YouDiscard
        effect = ModalEffect.chooseOneNotYetChosenThisTurn(
            Mode.noTarget(Effects.DrawCards(1), "Draw a card"),
            Mode.noTarget(Effects.CreateTreasure(1), "Create a Treasure token"),
            Mode.noTarget(
                Effects.LoseLife(3, EffectTarget.PlayerRef(Player.EachOpponent)),
                "Each opponent loses 3 life",
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "237"
        artist = "Victor Sales"
        flavorText = "Loss is not the end of the journey."
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d21433ba-0a14-42bc-ad0b-a4ef823a3295.jpg?1783907848"
        ruling(
            "2025-02-07",
            "If you discard a card after all three have been chosen in a turn, that instance of " +
                "the ability is removed from the stack with no effect."
        )
    }
}
