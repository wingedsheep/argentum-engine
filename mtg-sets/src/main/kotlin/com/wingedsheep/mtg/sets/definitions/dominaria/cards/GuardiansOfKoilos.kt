package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

val GuardiansOfKoilos = card("Guardians of Koilos") {
    manaCost = "{5}"
    typeLine = "Artifact Creature — Construct"
    oracleText = "When Guardians of Koilos enters the battlefield, you may return another target historic permanent you control to its owner's hand. (Artifacts, legendaries, and Sagas are historic.)"
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        optional = true
        val t = target("historic", TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Historic.youControl(), excludeSelf = true)
        ))
        effect = Effects.ReturnToHand(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "216"
        artist = "Yeong-Hao Han"
        flavorText = "Archaeologists depend on automatons inspired by their previous discoveries at Koilos to guard the excavations."
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbbc615a-708a-444b-acab-871cce22694d.jpg?1636491669"
    }
}
