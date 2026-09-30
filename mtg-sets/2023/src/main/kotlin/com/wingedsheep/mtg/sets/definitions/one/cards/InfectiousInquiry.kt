package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Infectious Inquiry — Phyrexia: All Will Be One #97
 * {2}{B}
 * Sorcery
 * You draw two cards and you lose 2 life. Each opponent gets a poison counter.
 *
 * The poison half is per-opponent — `AddCounters` resolves one player — so it runs under
 * `ForEachPlayer(EachOpponent)`, which rebinds `Player.You` to the visited opponent.
 */
val InfectiousInquiry = card("Infectious Inquiry") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "You draw two cards and you lose 2 life. Each opponent gets a poison counter."

    spell {
        effect = Effects.DrawCards(2) then
            Effects.LoseLife(2, EffectTarget.Controller) then
            Effects.ForEachPlayer(
                Player.EachOpponent,
                Effects.AddCounters(CounterType.POISON, 1, EffectTarget.PlayerRef(Player.You)),
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "97"
        artist = "Eli Minaya"
        flavorText = "\"I give you pain, you give me information. A simple trade, is it not?\""
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a10f284-b043-4307-bdc7-6dad47cc9221.jpg?1783918045"
    }
}
