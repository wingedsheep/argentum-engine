package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Urabrask's Anointer
 * {3}{R}
 * Artifact Creature — Phyrexian Wizard
 * 4/2
 *
 * When this creature enters, it deals X damage to any target, where X is the number of
 * permanents you control with oil counters on them.
 *
 * X is counted on resolution over every permanent you control carrying an oil counter
 * (the Anointer itself included, should it have one).
 */
val UrabrasksAnointer = card("Urabrask's Anointer") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Phyrexian Wizard"
    power = 4
    toughness = 2
    oracleText = "When this creature enters, it deals X damage to any target, where X is the number of " +
        "permanents you control with oil counters on them."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(Targets.Any)
        effect = Effects.DealDamage(
            DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Permanent.withCounter(CounterType.OIL),
            ).count(),
            t,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "152"
        artist = "Aaron J. Riley"
        flavorText = "\"You were so eager to spy on my work, I assume you'll appreciate a hands-on demonstration.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3ef034f-37e5-4baa-a7a9-5fd0de792535.jpg?1783918022"
    }
}
