package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val IronStar = card("Iron Star") {
    manaCost = "{1}"
    typeLine = "Artifact"
    oracleText = "Whenever a player casts a red spell, you may pay {1}. If you do, you gain 1 life."

    triggeredAbility {
        trigger = Triggers.anyPlayer.casts(GameObjectFilter.Any.withColor(Color.RED))
        effect = Effects.MayPay(ManaCost.parse("{1}"), Effects.GainLife(1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "250"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/5/7/5786de12-cade-43c2-a6b0-0c5b294b9d0e.jpg?1783948665"
    }
}
