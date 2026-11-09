package com.wingedsheep.mtg.sets.definitions.ala.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.RepeatCondition
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ad Nauseam — Shards of Alara #63 (canonical printing)
 * {3}{B}{B} · Instant
 *
 * Reveal the top card of your library and put that card into your hand. You lose life equal to its
 * mana value. You may repeat this process any number of times.
 *
 * Dark Confidant's reveal-and-lose pipeline as the body of a [Effects.RepeatWhile] — a do-while, so
 * the first pass is mandatory and the player is asked after each pass whether to go again
 * ([RepeatCondition.PlayerChooses], the Trade Secrets shape). Life is lost per pass, so the player
 * decides with the real life total in front of them.
 */
val AdNauseam = card("Ad Nauseam") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Reveal the top card of your library and put that card into your hand. You lose life equal to its mana value. You may repeat this process any number of times."

    spell {
        effect = Effects.RepeatWhile(
            body = Effects.Pipeline {
                val revealed = gather(CardSource.TopOfLibrary(1, Player.You))
                toHand(revealed, revealed = true)
                run(
                    Effects.LoseLife(
                        DynamicAmounts.manaValueOf(revealed),
                        EffectTarget.Controller,
                    )
                )
            },
            repeatCondition = RepeatCondition.PlayerChooses(
                decider = EffectTarget.Controller,
                prompt = "Reveal another card?",
                yesText = "Repeat",
                noText = "Stop",
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "63"
        artist = "Jeremy Jarvis"
        flavorText = "When the task spilled over into undeath, he stopped calling it his life's work."
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a4ce4a1-65e3-4b40-be35-8fc55a968ec8.jpg?1783942570"
    }
}
