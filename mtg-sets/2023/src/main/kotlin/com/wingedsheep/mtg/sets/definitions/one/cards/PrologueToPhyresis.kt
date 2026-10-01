package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Prologue to Phyresis — Phyrexia: All Will Be One #65
 * {1}{U}
 * Instant
 * Each opponent gets a poison counter.
 * Draw a card.
 *
 * The poison half is per-opponent — `AddCounters` resolves one player — so it runs under
 * `ForEachPlayer(EachOpponent)`, which rebinds `Player.You` to the visited opponent. The draw
 * follows outside that loop, so it is the caster's.
 */
val PrologueToPhyresis = card("Prologue to Phyresis") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Each opponent gets a poison counter.\nDraw a card."

    spell {
        effect = Effects.ForEachPlayer(
            Player.EachOpponent,
            Effects.AddCounters(CounterType.POISON, 1, EffectTarget.PlayerRef(Player.You)),
        ) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "65"
        artist = "Simon Dominic"
        flavorText = "\"Please, for science, tell me how it feels. Leave nothing out.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac625f30-ed91-4b21-ada8-aaa5b2ad79b8.jpg?1783918058"
    }
}
