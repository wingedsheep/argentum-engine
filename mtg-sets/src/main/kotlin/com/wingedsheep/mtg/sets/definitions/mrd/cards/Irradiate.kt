package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Irradiate — Mirrodin #67
 * {3}{B} · Instant
 *
 * Target creature gets -1/-1 until end of turn for each artifact you control.
 *
 * Modelling notes:
 * - [Effects.ModifyStats] with a [DynamicAmount] is the *resolution-time* shape: the artifact count
 *   is locked in when Irradiate resolves and the -N/-N sticks for the rest of the turn even if the
 *   artifacts are sacrificed in response to the death trigger. That is the printed behaviour —
 *   contrast [com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect], which keeps recomputing and
 *   belongs to permanents like Nim Devourer.
 * - Irradiate itself is on the stack (not the battlefield) while it resolves, so it never counts
 *   toward its own bonus; artifact *creatures* you control do count.
 */
val Irradiate = card("Irradiate") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets -1/-1 until end of turn for each artifact you control."

    spell {
        val t = target("target", Targets.Creature)
        val negArtifacts = DynamicAmount.Multiply(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count(),
            -1
        )
        effect = Effects.ModifyStats(negArtifacts, negArtifacts, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "67"
        artist = "Dave Dorman"
        flavorText = "The blast ignores the cage of metal but devours the flesh inside."
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e0460cf-ff87-4cf8-89b5-a8b9fb7322e0.jpg?1783944547"
    }
}
