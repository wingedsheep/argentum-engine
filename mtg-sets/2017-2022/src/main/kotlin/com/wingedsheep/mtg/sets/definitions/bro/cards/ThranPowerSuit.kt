package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.effects.WardCost

/**
 * Thran Power Suit
 * {2}
 * Artifact — Equipment
 * Equipped creature gets +1/+1 for each Aura and Equipment attached to it and has ward {2}.
 * Equip {2}
 *
 * The bonus counts every attachment on the *equipped creature* (only Auras and Equipment can be
 * attached to a creature), read through the source's attachment link — With Great Power's spelling,
 * [DynamicAmounts.attachmentsOnEnchantedCreature]. The Suit counts itself, so alone it is +1/+1.
 * Ward {2} is granted to the equipped creature with Chains of Custody's [GrantWard].
 */
val ThranPowerSuit = card("Thran Power Suit") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1 for each Aura and Equipment attached to it and has ward {2}. " +
        "(Whenever equipped creature becomes the target of a spell or ability an opponent controls, counter it " +
        "unless that player pays {2}.)\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = GrantDynamicStats(
            filter = Filters.EquippedCreature,
            powerBonus = DynamicAmounts.attachmentsOnEnchantedCreature(),
            toughnessBonus = DynamicAmounts.attachmentsOnEnchantedCreature()
        )
    }

    staticAbility { ability = GrantWard(WardCost.Mana("{2}")) }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "253"
        artist = "Raf Kayupov"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/17bfb8c5-9105-41e4-a58f-9c9eee32b18a.jpg?1783920012"
    }
}
