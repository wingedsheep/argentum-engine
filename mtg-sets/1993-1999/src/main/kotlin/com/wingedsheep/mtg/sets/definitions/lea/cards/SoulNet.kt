package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val SoulNet = card("Soul Net") {
    manaCost = "{1}"
    typeLine = "Artifact"
    oracleText = "Whenever a creature dies, you may pay {1}. If you do, you gain 1 life."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature).dies()
        effect = Effects.MayPay(ManaCost.parse("{1}"), Effects.GainLife(1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "270"
        artist = "Dameon Willich"
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b814198-814b-4619-a158-327af675f8f2.jpg?1783948661"
        ruling("2004-10-04", "If animated so it is a creature, it can be triggered off its own destruction.")
    }
}
