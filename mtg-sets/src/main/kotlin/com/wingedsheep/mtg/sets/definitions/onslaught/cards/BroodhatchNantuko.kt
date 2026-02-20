package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateDynamicTokensEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Broodhatch Nantuko
 * {1}{G}
 * Creature — Insect Druid
 * 1/1
 * Whenever Broodhatch Nantuko is dealt damage, create that many 1/1 green Insect creature tokens.
 * Morph {2}{G}
 */
val BroodhatchNantuko = card("Broodhatch Nantuko") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Insect Druid"
    power = 1
    toughness = 1
    oracleText = "Whenever Broodhatch Nantuko is dealt damage, create that many 1/1 green Insect creature tokens.\nMorph {2}{G}"

    triggeredAbility {
        trigger = Triggers.TakesDamage
        effect = CreateDynamicTokensEffect(
            count = DynamicAmount.TriggerDamageAmount,
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Insect"),
            imageUri = "https://cards.scryfall.io/normal/front/a/a/aa47df37-f246-4f80-a944-008cdf347dad.jpg?1561757793"
        )
    }

    morph = "{2}{G}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "250"
        artist = "Keith Garletts"
        flavorText = "\"We are the swarming horde.\""
        imageUri = "https://cards.scryfall.io/large/front/3/8/38315ba3-57a0-4aa0-b1bc-4b1fcdd763d4.jpg?1562908205"
    }
}
