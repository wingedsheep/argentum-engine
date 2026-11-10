package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Skeleton Key (Shadows over Innistrad #263)
 * {1}
 * Artifact — Equipment
 *
 * Equipped creature has skulk. (It can't be blocked by creatures with greater power.)
 * Whenever equipped creature deals combat damage to a player, you may draw a card. If you do, discard a card.
 * Equip {2}
 */
val SkeletonKey = card("Skeleton Key") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature has skulk. (It can't be blocked by creatures with greater power.)\n" +
        "Whenever equipped creature deals combat damage to a player, you may draw a card. If you do, discard a card.\n" +
        "Equip {2}"

    staticAbility {
        ability = GrantKeyword(Keyword.SKULK)
    }

    triggeredAbility {
        trigger = Triggers.attached.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.May(Patterns.Hand.loot())
        description = "Whenever equipped creature deals combat damage to a player, you may draw a card. " +
            "If you do, discard a card."
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "263"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/3168317b-b166-483e-9d9a-20cdfcdc255c.jpg?1783937707"
        ruling(
            "2016-04-08",
            "Skulk matters only as blockers are chosen. Modifying either creature's power after blockers " +
                "are chosen won't cause the attacking creature to become unblocked."
        )
    }
}
