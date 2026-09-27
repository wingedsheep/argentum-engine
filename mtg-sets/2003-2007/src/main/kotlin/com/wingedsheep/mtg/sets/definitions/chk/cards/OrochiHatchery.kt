package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters

/**
 * Orochi Hatchery
 * {X}{X}
 * Artifact
 *
 * This artifact enters with X charge counters on it.
 * {5}, {T}: Create a 1/1 green Snake creature token for each charge counter on this artifact.
 *
 * The token count is read from the charge counters at resolution, so counters added or removed
 * in response change how many Snakes are made.
 */
val OrochiHatchery = card("Orochi Hatchery") {
    manaCost = "{X}{X}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "This artifact enters with X charge counters on it.\n" +
        "{5}, {T}: Create a 1/1 green Snake creature token for each charge counter on this artifact."

    replacementEffect(
        EntersWithDynamicCounters(
            counterType = CounterType.CHARGE,
            count = DynamicAmounts.castX()
        )
    )

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}"), Costs.Tap)
        effect = Effects.CreateToken(
            count = DynamicAmounts.countersOnSelf(CounterType.CHARGE),
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Snake"),
        )
        description = "{5}, {T}: Create a 1/1 green Snake creature token for each charge counter on this artifact."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "266"
        artist = "Alex Horley-Orlandelli"
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e662e2e-f706-4d79-86ed-48b60787a5d0.jpg?1783944276"
    }
}
