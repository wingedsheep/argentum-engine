package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Invigorating Boon
 * {1}{G}
 * Enchantment
 * Whenever a player cycles a card, you may put a +1/+1 counter on target creature.
 */
val InvigoratingBoon = card("Invigorating Boon") {
    manaCost = "{1}{G}"
    typeLine = "Enchantment"
    oracleText = "Whenever a player cycles a card, you may put a +1/+1 counter on target creature."

    triggeredAbility {
        trigger = Triggers.AnyPlayerCycles
        val t = target("target", Targets.Creature)
        effect = MayEffect(
            AddCountersEffect(
                counterType = "+1/+1",
                count = 1,
                target = t
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "267"
        artist = "Edward P. Beard, Jr."
        flavorText = "\"The Mirari's echoes rang in the scouts' minds long after they had returned from the Krosan Forest.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c46f324b-63c6-4fb5-a80a-e9da51c3eb77.jpg?1562941431"
    }
}
