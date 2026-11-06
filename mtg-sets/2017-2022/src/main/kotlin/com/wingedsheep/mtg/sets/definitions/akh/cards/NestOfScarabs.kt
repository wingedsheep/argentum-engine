package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Nest of Scarabs
 * {2}{B}
 * Enchantment
 * Whenever you put one or more -1/-1 counters on a creature, create that many 1/1 black Insect
 * creature tokens.
 *
 * Same per-recipient trigger shape as Hapatra, Vizier of Poisons: "on **a** creature" fires once per
 * creature that got counters, any creature qualifies, and `placedBy = You` scopes it to counters you
 * put (CR 122.6a — wither damage counters are put by the source's controller). "That many" is the
 * number of counters that one placement put on that creature.
 */
val NestOfScarabs = card("Nest of Scarabs") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Whenever you put one or more -1/-1 counters on a creature, create that many 1/1 black Insect creature tokens."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature).getsCounters(CounterType.MINUS_ONE_MINUS_ONE, by = Player.You)
        effect = Effects.CreateToken(
            count = DynamicAmounts.triggerCountersPlaced(),
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Insect"),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "101"
        artist = "Jason A. Engle"
        flavorText = "\"They are small, but they always want more. Learn from them.\"\n—Bontu, god of ambition"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97aa6b45-055a-411e-b2db-b373a4d3826d.jpg?1783936503"
        ruling("2017-04-18", "If an effect has you put more -1/-1 counters on a creature than it has toughness, you'll put all of those counters on it and create that many Insects, even if that makes its toughness a negative number.")
        ruling("2017-04-18", "If a creature with wither or infect deals damage to a creature, the controller of the creature with wither or infect puts that many -1/-1 counters on the second creature.")
    }
}
