package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Make Your Mark — Strixhaven: School of Mages #204 (canonical printing)
 * {R/W} · Instant
 *
 * Target creature gets +1/+0 until end of turn. When that creature dies this turn, create a 3/2
 * red and white Spirit creature token.
 *
 * Blessed Defiance's shape: the death clause is an entity-scoped delayed trigger
 * (`Triggers.self.dies()` watching the chosen creature), `fireOnce` for the printed "When",
 * expiring at end of turn.
 */
val MakeYourMark = card("Make Your Mark") {
    manaCost = "{R/W}"
    colorIdentity = "RW"
    typeLine = "Instant"
    oracleText = "Target creature gets +1/+0 until end of turn. When that creature dies this turn, " +
        "create a 3/2 red and white Spirit creature token."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(1, 0, t) then
            Effects.CreateDelayedTrigger(
                effect = Effects.CreateToken(
                    power = 3,
                    toughness = 2,
                    colors = setOf(Color.RED, Color.WHITE),
                    creatureTypes = setOf("Spirit")
                ),
                trigger = Triggers.self.dies(),
                watchedTarget = t,
                fireOnce = true,
                expiry = DelayedTriggerExpiry.EndOfTurn
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "204"
        artist = "Bryan Sola"
        flavorText = "Those who do learn from history may still choose to repeat it."
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e937830-3804-44e8-b9d0-3e5f5cf39eab.jpg?1783927305"
    }
}
