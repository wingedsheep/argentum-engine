package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val SengirConnoisseur = card("Sengir Connoisseur") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    oracleText = "Flying\nWhenever one or more other creatures die, put a +1/+1 counter on this creature. This ability triggers only once each turn."
    power = 3
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.oneOrMoreOther(GameObjectFilter.Creature.anyController()).die()
        oncePerTurn = true
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "104"
        artist = "Irina Nordsol"
        flavorText = "\"We cannot afford to let the Phyrexians win. They taste terrible.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c9231f0-7733-46d9-b9c6-c6c5b7fe65dd.jpg?1783921328"
    }
}
