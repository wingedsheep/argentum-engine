package com.wingedsheep.mtg.sets.definitions.arb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Violent Outburst
 * {1}{R}{G}
 * Instant
 * Cascade
 * Creatures you control get +1/+0 until end of turn.
 *
 * [Keyword.CASCADE] is display-only; the cascade itself is the `Triggers.self.isCast()` trigger
 * feeding [Effects.Cascade] — the same shape as `arb/cards/EnlistedWurm.kt`.
 */
val ViolentOutburst = card("Violent Outburst") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Instant"
    oracleText = "Cascade (When you cast this spell, exile cards from the top of your library until you " +
        "exile a nonland card that costs less. You may cast it without paying its mana cost. Put " +
        "the exiled cards on the bottom in a random order.)\n" +
        "Creatures you control get +1/+0 until end of turn."

    keywords(Keyword.CASCADE)

    // Cascade — the cast trigger the keyword abbreviates.
    triggeredAbility {
        trigger = Triggers.self.isCast()
        effect = Effects.Cascade
        description = "Cascade"
    }

    spell {
        effect = Patterns.Group.modifyStatsForAll(1, 0, GroupFilter(GameObjectFilter.Creature.youControl()))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "63"
        artist = "Richard Whitters"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b58856a-f88e-4625-8636-62b5c717b956.jpg?1783942428"
    }
}
