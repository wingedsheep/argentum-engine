package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Glyph of Reincarnation — Legends #189
 * {G} · Instant
 *
 * Cast this spell only after combat.
 * Destroy all creatures that were blocked by target Wall this turn. They can't be regenerated. For
 * each creature that died this way, put a creature card from the graveyard of the player who
 * controlled that creature the last time it became blocked by that Wall onto the battlefield under
 * its owner's control.
 *
 * Builder's Bane's captured-controller pipeline: gather the creatures the Wall blocked this turn,
 * snapshot who controlled each one when the Wall last blocked it (`asLastBlockedBy` reads the block
 * record, not the current controller), destroy them without regeneration, then once per snapshotted
 * player the caster ([Chooser.SourceController]) picks as many creature cards from that player's
 * graveyard as their creatures died — all deaths happen first, so the picks may include the
 * creatures just destroyed (2004-10-04 ruling) — and they return under their owners' control.
 */
val GlyphOfReincarnation = card("Glyph of Reincarnation") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Cast this spell only after combat.\nDestroy all creatures that were blocked by target Wall this " +
        "turn. They can't be regenerated. For each creature that died this way, put a creature card from the " +
        "graveyard of the player who controlled that creature the last time it became blocked by that Wall onto " +
        "the battlefield under its owner's control."

    spell {
        castOnlyIf(Conditions.AfterCombat)
        val wall = target(TargetFilter(GameObjectFilter.Creature.withSubtype("Wall")))
        effect = Effects.Pipeline {
            val blocked = gather(GameObjectFilter.Creature.wasBlockedByThisTurn(wall))
            val controllers = captureControllers(blocked, asLastBlockedBy = wall)
            val died = moveTracked(
                blocked,
                CardDestination.ToZone(Zone.GRAVEYARD),
                moveType = MoveType.Destroy,
                noRegenerate = true
            )
            forEachCaptured(died, original = blocked, controllers = controllers) { deaths ->
                val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Creature))
                val returning = chooseExactly(
                    deaths.amount,
                    graveyard,
                    chooser = Chooser.SourceController,
                    prompt = "Choose creature cards to put onto the battlefield"
                )
                move(returning, CardDestination.ToZone(Zone.BATTLEFIELD), underOwnersControl = true)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "189"
        artist = "Susan Van Camp"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a67e8214-a192-4143-9d5e-d0e254e1bf6e.jpg"
    }
}
