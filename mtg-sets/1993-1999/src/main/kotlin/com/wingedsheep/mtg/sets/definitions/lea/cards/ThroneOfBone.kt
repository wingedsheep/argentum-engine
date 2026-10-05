package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val ThroneOfBone = card("Throne of Bone") {
    manaCost = "{1}"
    typeLine = "Artifact"
    oracleText = "Whenever a player casts a black spell, you may pay {1}. If you do, you gain 1 life."

    triggeredAbility {
        trigger = Triggers.anyPlayer.casts(GameObjectFilter.Any.withColor(Color.BLACK))
        effect = Effects.MayPay(ManaCost.parse("{1}"), Effects.GainLife(1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "273"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2931ae0-7836-4000-b9ec-f2029ebf5d96.jpg?1783948660"
    }
}
