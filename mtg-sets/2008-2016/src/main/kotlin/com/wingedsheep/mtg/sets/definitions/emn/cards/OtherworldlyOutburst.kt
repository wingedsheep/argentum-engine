package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Otherworldly Outburst — Eldritch Moon #138
 * {R} · Instant
 *
 * Target creature gets +1/+0 until end of turn. When that creature dies this turn, create a 3/2
 * colorless Eldrazi Horror creature token.
 *
 * Same shape as Felonious Rage / Turn Inside Out: the pump, then an entity-scoped delayed
 * `Triggers.self.dies()` bound to the target via `watchedTarget`, `fireOnce` for the printed
 * "When", expiring at end of turn ("this turn"). It fires on the creature's death regardless of
 * who controls it, and the token goes to the spell's controller. If the target is gone before
 * resolution the spell doesn't resolve and no delayed trigger is created (ruling 2016-07-13).
 * No token `imageUri` — the Eldrazi Horror art comes from Eldritch Moon's `temn` token printings.
 */
val OtherworldlyOutburst = card("Otherworldly Outburst") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target creature gets +1/+0 until end of turn. When that creature dies this turn, " +
        "create a 3/2 colorless Eldrazi Horror creature token."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(1, 0, t) then
            Effects.CreateDelayedTrigger(
                effect = Effects.CreateToken(
                    power = 3,
                    toughness = 2,
                    colors = emptySet(),
                    creatureTypes = setOf("Eldrazi", "Horror")
                ),
                trigger = Triggers.self.dies(),
                watchedTarget = t,
                fireOnce = true,
                expiry = DelayedTriggerExpiry.EndOfTurn
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "138"
        artist = "Kieran Yanner"
        flavorText = "A tiny crack is enough to open the mind to Emrakul."
        imageUri = "https://cards.scryfall.io/normal/front/3/4/34409e34-04e9-4279-8d21-6ef362b20b72.jpg?1783937462"
        ruling(
            "2016-07-13",
            "If the targeted creature dies before Otherworldly Outburst resolves, the spell doesn't " +
                "resolve. You won't get an Eldrazi Horror token."
        )
        ruling(
            "2016-07-13",
            "If the targeted creature doesn't die this turn, the delayed triggered ability simply won't trigger."
        )
    }
}
