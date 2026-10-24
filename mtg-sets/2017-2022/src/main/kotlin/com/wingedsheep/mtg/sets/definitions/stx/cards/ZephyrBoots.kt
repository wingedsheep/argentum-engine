package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Zephyr Boots — Strixhaven: School of Mages #261 (canonical printing)
 * {1} · Artifact — Equipment
 *
 * Equipped creature has flying.
 * Whenever equipped creature deals combat damage to a player, draw a card, then discard a card.
 * Equip {2}
 *
 * Neurok Hoversail's attached-creature [GrantKeyword] plus Mask of Memory's
 * `Triggers.attached.dealsCombatDamage(AnyPlayer)` trigger, paying off a mandatory
 * [Patterns.Hand.loot].
 */
val ZephyrBoots = card("Zephyr Boots") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature has flying.\n" +
        "Whenever equipped creature deals combat damage to a player, draw a card, then discard a card.\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING)
    }

    triggeredAbility {
        trigger = Triggers.attached.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Patterns.Hand.loot(draw = 1, discard = 1)
        description = "Whenever equipped creature deals combat damage to a player, draw a card, then discard a card."
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "261"
        artist = "YW Tang"
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d0566fd-3775-48f1-ba22-6e659558c3d3.jpg?1783927277"
    }
}
