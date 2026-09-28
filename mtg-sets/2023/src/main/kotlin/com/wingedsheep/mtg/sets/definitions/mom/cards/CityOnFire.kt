package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DoubleDamage
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * City on Fire
 * {5}{R}{R}{R}
 * Enchantment
 *
 * Convoke
 * If a source you control would deal damage to a permanent or player, it deals triple that
 * damage instead.
 *
 * The damage-scaling replacement (CR 614.1a) is [DoubleDamage] with `multiplier = 3`; "a permanent
 * or player" is the unscoped [Recipient.Any], so combat and noncombat damage to anything —
 * your own permanents included — triples.
 */
val CityOnFire = card("City on Fire") {
    manaCost = "{5}{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while " +
        "casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "If a source you control would deal damage to a permanent or player, it deals triple " +
        "that damage instead."

    keywords(Keyword.CONVOKE)

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
        rarity = Rarity.RARE
        collectorNumber = "135"
        artist = "Jake Murray"
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f455cc1-a822-44ef-ba7c-bfcff69bd45e.jpg?1783916996"
    }
}
