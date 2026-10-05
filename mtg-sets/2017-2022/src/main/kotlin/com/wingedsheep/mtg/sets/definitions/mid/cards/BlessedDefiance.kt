package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Blessed Defiance — Innistrad: Midnight Hunt #5
 * {W} · Instant
 *
 * Target creature you control gets +2/+0 and gains lifelink until end of turn. When that creature
 * dies this turn, create a 1/1 white Spirit creature token with flying.
 *
 * Same shape as Felonious Rage: the death clause is an entity-scoped delayed triggered ability
 * (`Triggers.self.dies()` with `watchedTarget` bound to the chosen creature), independent of the
 * pump — it fires for any death that turn, not only a combat death. `fireOnce` matches the printed
 * "When", and `DelayedTriggerExpiry.EndOfTurn` is the "this turn" scope.
 *
 * Reprinted in Innistrad: Double Feature (DBL) and Jumpstart 2022 (J22, `Printing` row).
 */
val BlessedDefiance = card("Blessed Defiance") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature you control gets +2/+0 and gains lifelink until end of turn. " +
        "When that creature dies this turn, create a 1/1 white Spirit creature token with flying."

    spell {
        val t = target(TargetFilter.CreatureYouControl)
        effect = Effects.ModifyStats(2, 0, t) then
            Effects.GrantKeyword(Keyword.LIFELINK, t) then
            Effects.CreateDelayedTrigger(
                effect = Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.WHITE),
                    creatureTypes = setOf("Spirit"),
                    keywords = setOf(Keyword.FLYING)
                ),
                trigger = Triggers.self.dies(),
                watchedTarget = t,
                fireOnce = true,
                expiry = DelayedTriggerExpiry.EndOfTurn
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "5"
        artist = "Kim Sokol"
        flavorText = "\"If I'm going, I'm taking you with me!\""
        imageUri = "https://cards.scryfall.io/normal/front/1/5/1555cdcb-3471-4ee7-99c3-d05311bf433c.jpg?1783925665"
    }
}
