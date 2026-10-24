package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Prismari Command — Strixhaven: School of Mages #214 (canonical printing)
 * {1}{U}{R} · Instant
 *
 * Choose two —
 * • Prismari Command deals 2 damage to any target.
 * • Target player draws two cards, then discards two cards.
 * • Target player creates a Treasure token.
 * • Destroy target artifact.
 *
 * A `modal(chooseCount = 2)` spell with per-mode targets. The loot mode is the Cephalid Broker
 * shape; the Treasure mode hands the token to the targeted player via `controller`.
 */
val PrismariCommand = card("Prismari Command") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Instant"
    oracleText =
        "Choose two —\n" +
        "• Prismari Command deals 2 damage to any target.\n" +
        "• Target player draws two cards, then discards two cards.\n" +
        "• Target player creates a Treasure token.\n" +
        "• Destroy target artifact."

    spell {
        modal(chooseCount = 2) {
            mode("Prismari Command deals 2 damage to any target") {
                val victim = target(Targets.Any)
                effect = Effects.DealDamage(2, victim)
            }
            mode("Target player draws two cards, then discards two cards") {
                val player = target(Targets.Player)
                effect = Effects.DrawCards(2, player) then Patterns.Hand.discardCards(2, player)
            }
            mode("Target player creates a Treasure token") {
                val player = target(Targets.Player)
                effect = Effects.CreateTreasure(controller = player)
            }
            mode("Destroy target artifact") {
                val artifact = target(TargetFilter.Artifact)
                effect = Effects.Destroy(artifact)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "214"
        artist = "Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/866b7fd4-86e3-4b42-b1ea-33bad0db1f9f.jpg?1783927301"
    }
}
