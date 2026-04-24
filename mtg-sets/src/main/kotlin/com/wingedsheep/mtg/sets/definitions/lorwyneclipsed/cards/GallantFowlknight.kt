package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val GallantFowlknight = card("Gallant Fowlknight") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Kithkin Knight"
    power = 3
    toughness = 4
    oracleText = "When this creature enters, creatures you control get +1/+0 until end of turn. " +
        "Kithkin creatures you control also gain first strike until end of turn."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ForEachInGroupEffect(
            GroupFilter.AllCreaturesYouControl,
            ModifyStatsEffect(1, 0, EffectTarget.Self)
        ) then ForEachInGroupEffect(
            GroupFilter.AllCreaturesYouControl.withSubtype(Subtype.KITHKIN),
            GrantKeywordEffect(Keyword.FIRST_STRIKE, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "17"
        artist = "Edgar Sánchez Hidalgo"
        flavorText = "The thoughtweft amplifies any small bud of courage, turning fear to resolve and daring to heroics."
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb6096ba-8083-4207-9a3f-c1e4ff095204.jpg?1767732475"
    }
}
