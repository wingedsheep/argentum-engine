package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Feed the Infection
 * {3}{B}
 * Sorcery
 * You draw three cards and you lose 3 life.
 * Corrupted — Each opponent who has three or more poison counters loses 3 life.
 *
 * "Each opponent who …" is a per-opponent test: `ForEachPlayer(EachOpponent)` rebinds `Player.You`
 * to the opponent being visited, so [Conditions.PoisonCountersAtLeast] reads *that* opponent's
 * poison and the life loss lands on them.
 */
val FeedTheInfection = card("Feed the Infection") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "You draw three cards and you lose 3 life.\n" +
        "Corrupted — Each opponent who has three or more poison counters loses 3 life."

    spell {
        effect = Effects.DrawCards(3) then
            Effects.LoseLife(3, EffectTarget.Controller) then
            Effects.ForEachPlayer(
                Player.EachOpponent,
                Effects.If(
                    condition = Conditions.PoisonCountersAtLeast(3),
                    then = Effects.LoseLife(3, EffectTarget.PlayerRef(Player.You)),
                ),
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "93"
        artist = "Jason A. Engle"
        flavorText = "They wished to become vital parts of the Dross Pits. Azax-Azog granted their request."
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f013a1a-d4b9-4380-9802-c299ee6c4492.jpg?1783918046"
    }
}
