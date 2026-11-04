package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mummy Paramount — bare "Zombie" means any Zombie permanent you control (the Wayward Servant
 * shape), and "another" is the OTHER trigger binding.
 */
val MummyParamount = card("Mummy Paramount") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Zombie"
    oracleText = "Whenever another Zombie you control enters, this creature gets +1/+1 until end of turn."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Permanent.withSubtype(Subtype.ZOMBIE).youControl()).enters()
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "16"
        artist = "Josu Hernaiz"
        flavorText = "\"It appears some mummies have begun to observe a hierarchy all their own.\"\n—Elekh, vizier of embalming"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8b1a3e5-cf77-439d-8e0d-cf6571f6891c.jpg?1783936060"
    }
}
