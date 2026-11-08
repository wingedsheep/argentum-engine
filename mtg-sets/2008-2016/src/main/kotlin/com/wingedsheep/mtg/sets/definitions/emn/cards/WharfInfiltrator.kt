package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Wharf Infiltrator (Eldritch Moon #80)
 * {1}{U}
 * Creature — Human Horror
 * 1/1
 *
 * Skulk (This creature can't be blocked by creatures with greater power.)
 * Whenever this creature deals combat damage to a player, you may draw a card. If you do, discard a card.
 * Whenever you discard a creature card, you may pay {2}. If you do, create a 3/2 colorless Eldrazi
 * Horror creature token.
 *
 * - The discard trigger fires once per creature card you discard, from any source — the loot here,
 *   a cost, cleanup — not just this card's own loot (ruling 2016-07-13).
 * - One payment of {2} per trigger, one token; no Eldrazi Horror `imageUri` — the token art comes from
 *   Eldritch Moon's `temn` token printings.
 */
val WharfInfiltrator = card("Wharf Infiltrator") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Horror"
    power = 1
    toughness = 1
    oracleText = "Skulk (This creature can't be blocked by creatures with greater power.)\n" +
        "Whenever this creature deals combat damage to a player, you may draw a card. If you do, discard a card.\n" +
        "Whenever you discard a creature card, you may pay {2}. If you do, create a 3/2 colorless " +
        "Eldrazi Horror creature token."

    keywords(Keyword.SKULK)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.May(Patterns.Hand.loot())
    }

    triggeredAbility {
        trigger = Triggers.you.discards(GameObjectFilter.Creature)
        effect = Effects.MayPay(
            ManaCost.parse("{2}"),
            Effects.CreateToken(
                power = 3,
                toughness = 2,
                colors = emptySet(),
                creatureTypes = setOf("Eldrazi", "Horror")
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "80"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/264baa9a-67fc-4903-9ed8-a4ce6912a184.jpg?1783937488"
        ruling(
            "2016-07-13",
            "Wharf Infiltrator's last ability triggers anytime you discard a creature card, not just when " +
                "you discard a card because of Wharf Infiltrator's other triggered ability."
        )
        ruling(
            "2016-07-13",
            "While resolving Wharf Infiltrator's last ability, you can't pay {2} multiple times to get " +
                "multiple Eldrazi Horror tokens."
        )
    }
}
