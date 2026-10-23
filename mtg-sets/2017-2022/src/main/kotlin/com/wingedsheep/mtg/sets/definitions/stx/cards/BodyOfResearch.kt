package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Body of Research — Strixhaven: School of Mages #168 (canonical printing)
 * {G}{G}{G}{U}{U}{U} · Sorcery
 *
 * Create a 0/0 green and blue Fractal creature token. Put X +1/+1 counters on it, where X is the
 * number of cards in your library.
 *
 * The Fractal is created (published to [CREATED_TOKENS]), then X +1/+1 counters land on it via
 * `PipelineTarget(CREATED_TOKENS, 0)` — the Fractal Anomaly / Wild Hypothesis shape. X is the
 * controller's library size, read at resolution. Token art resolves through STX's synced token
 * printings.
 */
val BodyOfResearch = card("Body of Research") {
    manaCost = "{G}{G}{G}{U}{U}{U}"
    colorIdentity = "GU"
    typeLine = "Sorcery"
    oracleText = "Create a 0/0 green and blue Fractal creature token. Put X +1/+1 counters on it, " +
        "where X is the number of cards in your library."

    spell {
        effect = Effects.CreateToken(
            power = 0,
            toughness = 0,
            colors = setOf(Color.GREEN, Color.BLUE),
            creatureTypes = setOf(Subtype.FRACTAL.value)
        ) then
            Effects.AddDynamicCounters(
                CounterType.PLUS_ONE_PLUS_ONE,
                DynamicAmounts.zone(Player.You, Zone.LIBRARY).count(),
                EffectTarget.PipelineTarget(CREATED_TOKENS, 0)
            )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "168"
        artist = "Antonio José Manzanedo"
        flavorText = "Needledeep had promised that his final project would be his biggest one yet."
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98b56823-0076-49cc-b5aa-4accb8e2782e.jpg?1783927322"
    }
}
