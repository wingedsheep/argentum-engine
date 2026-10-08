package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DoubleDamage
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Fiery Emancipation
 * {3}{R}{R}{R}
 * Enchantment
 *
 * If a source you control would deal damage to a permanent or player, it deals triple that damage
 * to that permanent or player instead.
 *
 * The damage-scaling replacement (CR 614.1a) is [DoubleDamage] with `multiplier = 3` — the same
 * script as City on Fire's tripling line. "A permanent or player" is the unscoped [Recipient.Any],
 * so combat and noncombat damage to anything, your own permanents included, triples. Trample and
 * divided damage are assigned unmodified and tripled afterwards (rulings), which is where the
 * replacement runs.
 */
val FieryEmancipation = card("Fiery Emancipation") {
    manaCost = "{3}{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "If a source you control would deal damage to a permanent or player, it deals triple " +
        "that damage to that permanent or player instead."

    replacementEffect(
        DoubleDamage(
            appliesTo = EventPattern.DamageEvent(
                source = GameObjectFilter.Any.youControl(),
                recipient = Recipient.Any,
            ),
            multiplier = 3,
        )
    )

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "143"
        artist = "Alexander Forssberg"
        flavorText = "\"One day the mountain's fire will rise up to burn away our chains and end this age of oppression.\"\n—*The Molten Prophecy*"
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b12e4b7-2c45-4795-94d3-901f89b8f290.jpg?1783930692"
        ruling(
            "2023-09-01",
            "If multiple replacement or prevention effects try to modify damage that would be dealt to a player " +
                "or permanent, the player or the controller of the permanent chooses the order in which they apply."
        )
        ruling(
            "2020-06-23",
            "If a creature with trample you control would deal combat damage to a blocking creature while you " +
                "control Fiery Emancipation, you must assign its unmodified damage. For example, a 3/3 creature " +
                "with trample blocked by a 2/2 creature can have 2 damage assigned to the blocking creature and " +
                "1 damage assigned to the defending player. It will then deal 6 damage to the blocking creature " +
                "and 3 damage to the defending player."
        )
        ruling(
            "2020-06-23",
            "If an effect asks you to divide damage among targets, you must divide the unmodified damage before tripling it."
        )
    }
}
