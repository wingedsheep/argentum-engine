package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val YavimayaIconoclast = card("Yavimaya Iconoclast") {
    manaCost = "{1}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Elf"
    oracleText = "Kicker {R} (You may pay an additional {R} as you cast this spell.)\nTrample\nWhen this creature enters, if it was kicked, it gets +1/+1 and gains haste until end of turn."
    power = 3
    toughness = 2

    keywordAbility(KeywordAbility.kicker("{R}"))
    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasKicked
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.HASTE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "190"
        artist = "Caio Monteiro"
        flavorText = "Steel weapons hold a forbidden allure to some of the younger Yavimayans."
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b1af9be-7f5e-48e9-a6e7-9261e199812e.jpg?1783921287"
    }
}
