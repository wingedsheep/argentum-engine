package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Zenith Flare
 * {2}{R}{W}
 * Instant
 *
 * Zenith Flare deals X damage to any target and you gain X life, where X is the number of cards
 * with a cycling ability in your graveyard.
 *
 * X is a [DynamicAmounts.count] over your graveyard with `withCycling()` (typecycling counts too,
 * CR 702.29e), read on resolution — Zenith Flare itself is still on the stack then.
 */
val ZenithFlare = card("Zenith Flare") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Instant"
    oracleText = "Zenith Flare deals X damage to any target and you gain X life, where X is the " +
        "number of cards with a cycling ability in your graveyard."

    spell {
        val t = target(Targets.Any)
        val x = DynamicAmounts.count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Any.withCycling())
        effect = Effects.DealDamage(x, t) then Effects.GainLife(x)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "217"
        artist = "Jonas De Ro"
        flavorText = "\"I defy this world to try our defenses now. Send pterons and skycats. Send Vadrok!\"\n" +
            "—Janae, Coppercoat architect"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0efac1ed-3f01-487c-86be-8239568b4425.jpg?1783931012"
        ruling("2020-04-17", "If the chosen target is an illegal target by the time Zenith Flare tries to resolve, the spell won't resolve. You won't gain X life.")
    }
}
