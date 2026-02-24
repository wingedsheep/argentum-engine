package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.DrawUpToEffect
import com.wingedsheep.sdk.scripting.effects.RepeatCondition
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

/**
 * Trade Secrets
 * {1}{U}{U}
 * Sorcery
 * Target opponent draws two cards, then you draw up to four cards.
 * That opponent may repeat this process as many times as they choose.
 */
val TradeSecrets = card("Trade Secrets") {
    manaCost = "{1}{U}{U}"
    typeLine = "Sorcery"
    oracleText = "Target opponent draws two cards, then you draw up to four cards. That opponent may repeat this process as many times as they choose."

    spell {
        val t = target("target", TargetOpponent())
        effect = Effects.RepeatWhile(
            body = Effects.Composite(
                DrawCardsEffect(count = 2, target = t),
                DrawUpToEffect(maxCards = 4, target = EffectTarget.Controller)
            ),
            repeatCondition = RepeatCondition.PlayerChooses(
                decider = t,
                prompt = "Repeat the process? (You draw 2 cards, opponent draws up to 4)",
                yesText = "Repeat",
                noText = "Stop"
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "118"
        artist = "Ron Spears"
        flavorText = "\"The Cabal loves only secrets.\" —Empress Llawan"
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e92e197e-ef7e-46bb-9533-5f9819d545b2.jpg?1562945484"
    }
}
