package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Lifeblood
 * {2}{W}{W}
 * Enchantment
 *
 * Whenever a Mountain an opponent controls becomes tapped, you gain 1 life.
 */
val Lifeblood = card("Lifeblood") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Whenever a Mountain an opponent controls becomes tapped, you gain 1 life."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.withSubtype(Subtype.MOUNTAIN).opponentControls()).becomesTapped()
        effect = Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "27"
        artist = "Mark Tedin"
        flavorText = "\"Foolish wizard! As you tap the power of your lofty keep, so grows my strength.\"\n—Malvern Xelionos, *Letters*"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4ecb1362-9a67-4d4c-8d69-9ac2ebf4d0b0.jpg?1783948083"
    }
}
