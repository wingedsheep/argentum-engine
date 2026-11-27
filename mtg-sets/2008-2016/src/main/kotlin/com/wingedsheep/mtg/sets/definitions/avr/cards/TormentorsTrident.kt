package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.MustAttack

/**
 * Tormentor's Trident
 * {2}
 * Artifact — Equipment
 * Equipped creature gets +3/+0 and attacks each combat if able.
 * Equip {3}
 */
val TormentorsTrident = card("Tormentor's Trident") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +3/+0 and attacks each combat if able.\n" +
        "Equip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility { ability = ModifyStats(3, 0, Filters.EquippedCreature) }
    staticAbility { ability = MustAttack(Filters.EquippedCreature) }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "222"
        artist = "Anthony Palumbo"
        flavorText = "To a demon there is no such thing as restraint."
        imageUri = "https://cards.scryfall.io/normal/front/9/5/9543d454-27d6-42ba-aad8-54811d180cfb.jpg?1783940651"
        ruling("2012-05-01", "You still decide which player or planeswalker the equipped creature attacks.")
        ruling(
            "2012-05-01",
            "If, during your declare attackers step, the equipped creature is tapped, is affected by a spell or " +
                "ability that says it can't attack, or hasn't been under your control continuously since the turn " +
                "began (and doesn't have haste), then it doesn't attack. If there's a cost associated with having a " +
                "creature attack, you're not forced to pay that cost, so it doesn't have to attack in that case either.",
        )
    }
}
