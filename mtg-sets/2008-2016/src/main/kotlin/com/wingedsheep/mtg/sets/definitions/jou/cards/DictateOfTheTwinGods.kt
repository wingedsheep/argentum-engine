package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DoubleDamage
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Dictate of the Twin Gods
 * {3}{R}{R}
 * Enchantment
 * Flash
 * If a source would deal damage to a permanent or player, it deals double that damage to that
 * permanent or player instead.
 *
 * [DoubleDamage] over every damage event — any source, any controller, any recipient (Angrath's
 * Marauders without the "you control" restriction). Multiple copies stack multiplicatively.
 */
val DictateOfTheTwinGods = card("Dictate of the Twin Gods") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Flash\nIf a source would deal damage to a permanent or player, it deals double that damage to that permanent or player instead."

    keywords(Keyword.FLASH)

    replacementEffect(
        DoubleDamage(
            appliesTo = EventPattern.DamageEvent(
                source = GameObjectFilter.Any,
                recipient = Recipient.Any,
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "93"
        artist = "Jaime Jones"
        flavorText = "Iroas and Mogis are as different in appearance as they are in personality."
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69cc93f2-8198-45b1-9193-7cfd1cf7ced4.jpg?1783939427"
        ruling("2014-04-26", "Dictate of the Twin Gods applies to any damage, not just combat damage. It also doesn't matter who controls the source of the damage that's being dealt.")
        ruling("2014-04-26", "If more than one Dictate of the Twin Gods is on the battlefield, damage dealt will double for each one (two of them will end up multiplying the damage by four, three of them by eight, and four of them by sixteen).")
        ruling("2014-04-26", "If multiple effects modify how damage will be dealt, the player being dealt damage or the controller of the permanent being dealt damage chooses the order to apply the effects.")
    }
}
