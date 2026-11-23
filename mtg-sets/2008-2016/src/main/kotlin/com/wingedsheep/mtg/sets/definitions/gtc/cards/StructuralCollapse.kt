package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CategoryPickPurpose
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Structural Collapse — Gatecrash #107
 * {5}{R} · Sorcery
 *
 * Target player sacrifices an artifact and a land of their choice. Structural Collapse deals 2
 * damage to that player.
 *
 * Release's overlapping-category edict aimed at one player: the target player picks an artifact and
 * a land from their own permanents (`distinctPicks` — an artifact land answers one category only,
 * and they must give up two permanents whenever their board allows), then both go at once. The
 * damage follows whether or not anything was sacrificed.
 */
val StructuralCollapse = card("Structural Collapse") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Target player sacrifices an artifact and a land of their choice. " +
        "Structural Collapse deals 2 damage to that player."

    spell {
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            val pool = gather(CardSource.ControlledPermanents(Player.TargetPlayer, GameObjectFilter.Permanent))
            val doomed = chooseOnePerCategory(
                pool,
                listOf(GameObjectFilter.Artifact, GameObjectFilter.Land),
                purpose = CategoryPickPurpose.SACRIFICE,
                distinctPicks = true,
            )
            sacrifice(doomed)
        } then Effects.DealDamage(2, player)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "107"
        artist = "Sam Burley"
        flavorText = "The Gruul holiday of Rauck-Chauv always ends the same way."
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d10da484-db67-4afc-90ef-6caf7d2e3a75.jpg?1783940122"
    }
}
