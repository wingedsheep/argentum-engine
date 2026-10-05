package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val IvoryCup = card("Ivory Cup") {
    manaCost = "{1}"
    typeLine = "Artifact"
    oracleText = "Whenever a player casts a white spell, you may pay {1}. If you do, you gain 1 life."

    triggeredAbility {
        trigger = Triggers.anyPlayer.casts(GameObjectFilter.Any.withColor(Color.WHITE))
        effect = Effects.MayPay(ManaCost.parse("{1}"), Effects.GainLife(1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "251"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/9964d8d8-dc97-4e5f-9f52-173f7e2c37fd.jpg?1783948666"
    }
}
