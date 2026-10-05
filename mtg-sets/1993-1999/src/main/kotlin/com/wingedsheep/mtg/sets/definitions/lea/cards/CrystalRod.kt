package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val CrystalRod = card("Crystal Rod") {
    manaCost = "{1}"
    typeLine = "Artifact"
    oracleText = "Whenever a player casts a blue spell, you may pay {1}. If you do, you gain 1 life."

    triggeredAbility {
        trigger = Triggers.anyPlayer.casts(GameObjectFilter.Any.withColor(Color.BLUE))
        effect = Effects.MayPay(ManaCost.parse("{1}"), Effects.GainLife(1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "239"
        artist = "Amy Weber"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76693233-7961-4b7e-80f2-ed90e494c4aa.jpg?1783948668"
    }
}
