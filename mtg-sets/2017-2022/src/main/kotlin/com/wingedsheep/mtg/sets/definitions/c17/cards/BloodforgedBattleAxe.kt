package com.wingedsheep.mtg.sets.definitions.c17.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Bloodforged Battle-Axe — Commander 2017 #50
 * {1} · Artifact — Equipment
 *
 * Equipped creature gets +2/+0.
 * Whenever equipped creature deals combat damage to a player, create a token that's a copy of
 * this Equipment.
 * Equip {2}
 *
 * The copy comes from [Effects.CreateTokenCopyOfSelf]: it copies the Equipment's copiable values
 * (all three abilities, so each token snowballs too) and enters unattached, as the rulings say.
 */
val BloodforgedBattleAxe = card("Bloodforged Battle-Axe") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+0.\n" +
        "Whenever equipped creature deals combat damage to a player, create a token that's a copy of this Equipment.\n" +
        "Equip {2}"

    staticAbility {
        ability = ModifyStats(2, 0, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attached.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.CreateTokenCopyOfSelf()
        description = "Whenever equipped creature deals combat damage to a player, create a token that's a copy of this Equipment."
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "50"
        artist = "Alayna Danner"
        flavorText = "Spilled blood always begets more of the same."
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7f22249-579f-433d-9422-29d4e82ddc49.jpg?1783935932"
        ruling("2017-08-25", "The token enters the battlefield unattached.")
        ruling("2017-08-25", "The token has all three of Bloodforged Battle-Axe's abilities, including the one that creates more tokens.")
    }
}
