package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Typhoon — Legends #209
 * {2}{G} · Sorcery
 *
 * Typhoon deals damage to each opponent equal to the number of Islands that player controls.
 *
 * A per-opponent count: `ForEachPlayer(EachOpponent)` rebinds [Player.You] / [EffectTarget.Controller]
 * to the visited opponent, so each one is dealt damage equal to the Islands *they* control, counted
 * on resolution (2004-10-04 ruling) — Karma's Swamp count, swept over opponents.
 */
val Typhoon = card("Typhoon") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Typhoon deals damage to each opponent equal to the number of Islands that player controls."

    spell {
        effect = Effects.ForEachPlayer(
            Player.EachOpponent,
            Effects.DealDamage(
                DynamicAmounts.battlefield(
                    Player.You,
                    GameObjectFilter.Land.withSubtype(Subtype.ISLAND),
                ).count(),
                EffectTarget.Controller,
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "209"
        artist = "Anson Maddocks"
        flavorText = "Fierce winds ripped across the tropical landscape. What they did not destroy with " +
            "their fiery breath was washed away by torrential rain."
        imageUri = "https://cards.scryfall.io/normal/front/2/5/254e0403-67d8-4e73-8d89-c901ebeba49f.jpg?1783948043"
        ruling("2004-10-04", "Number of Islands is counted on resolution and not on announcement.")
    }
}
