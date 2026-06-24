package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.sneak
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Raphael's Technique
 * {4}{R}{R}
 * Instant
 *
 * Sneak {2}{R} (You may cast this spell for {2}{R} if you also return an unblocked attacker
 * you control to hand during the declare blockers step.)
 * Each player may discard their hand and draw seven cards.
 *
 * A per-player optional wheel, in APNAP order: [ForEachPlayerEffect] over [Player.Each] iterates
 * every player and rebinds the body's controller to the current player, so `Patterns.Hand.discardHand`
 * (of `EffectTarget.Controller`) and [Effects.DrawCards] act on them. Each player's body is wrapped
 * in [MayEffect] with `decisionMaker = Controller`, so every player independently chooses yes/no;
 * declining means no discard and no draw, honoring "may … and …" as one combined optional action.
 * Same shape as Step Between Worlds.
 */
val RaphaelsTechnique = card("Raphael's Technique") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Sneak {2}{R} (You may cast this spell for {2}{R} if you also return an unblocked attacker you control to hand during the declare blockers step.)\nEach player may discard their hand and draw seven cards."

    sneak("{2}{R}")

    spell {
        effect = ForEachPlayerEffect(
            players = Player.Each,
            effects = listOf(
                MayEffect(
                    decisionMaker = EffectTarget.Controller,
                    effect = Effects.Composite(
                        Patterns.Hand.discardHand(EffectTarget.Controller),
                        Effects.DrawCards(7)
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "105"
        artist = "Andreas Zafiratos"
        flavorText = "\"Change of plans!\""
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7ce8b00f-5a4f-4206-8eb3-e79308e91f47.jpg?1760102758"
    }
}
