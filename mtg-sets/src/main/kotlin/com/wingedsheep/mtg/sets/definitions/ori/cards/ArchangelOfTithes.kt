package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AttackTax
import com.wingedsheep.sdk.scripting.BlockTax
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Archangel of Tithes
 * {1}{W}{W}{W}
 * Creature — Angel
 * 3/5
 * Flying
 * As long as Archangel of Tithes is untapped, creatures can't attack you or planeswalkers you
 * control unless their controller pays {1} for each of those creatures.
 * As long as Archangel of Tithes is attacking, creatures can't block unless their controller
 * pays {1} for each of those creatures.
 *
 * Canonical earliest printing: Magic Origins (ORI, 2015). Reprinted in OTJ — see the OTJ
 * `Printing` rows.
 *
 * The attack tax is gated on the source being untapped via [Conditions.SourceIsUntapped]; the
 * block tax is gated on the source attacking via [Conditions.SourceIsAttacking]. Both protect
 * against tapping the controller's mana without consent (combat tax confirmation pause).
 */
val ArchangelOfTithes = card("Archangel of Tithes") {
    manaCost = "{1}{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    oracleText = "Flying\nAs long as this creature is untapped, creatures can't attack you or planeswalkers you control unless their controller pays {1} for each of those creatures.\nAs long as this creature is attacking, creatures can't block unless their controller pays {1} for each of those creatures."
    power = 3
    toughness = 5

    keywords(Keyword.FLYING)

    staticAbility {
        ability = AttackTax(
            amountPerAttacker = DynamicAmount.Fixed(1),
            condition = Conditions.SourceIsUntapped,
        )
    }

    staticAbility {
        ability = BlockTax(
            amountPerBlocker = DynamicAmount.Fixed(1),
            condition = Conditions.SourceIsAttacking,
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "4"
        artist = "Cynthia Sheppard"
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1af50bf1-c51e-4592-86bf-4197ec85a45d.jpg?1562009225"
    }
}
