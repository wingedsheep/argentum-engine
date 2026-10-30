package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Burning-Yard Trainer
 * {4}{R}
 * Creature — Human Knight
 * 3/3
 *
 * Trample, haste
 * When this creature enters, another target Knight you control gets +2/+2 and gains trample and
 * haste until end of turn.
 */
val BurningYardTrainer = card("Burning-Yard Trainer") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Knight"
    oracleText = "Trample, haste\n" +
        "When this creature enters, another target Knight you control gets +2/+2 and gains trample and haste until end of turn."
    power = 3
    toughness = 3

    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(
            TargetFilter(GameObjectFilter.Permanent.withSubtype("Knight").youControl(), excludeSelf = true)
        )
        effect = Effects.ModifyStats(2, 2, t) then
            Effects.GrantKeyword(Keyword.TRAMPLE, t) then
            Effects.GrantKeyword(Keyword.HASTE, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "117"
        artist = "Yongjae Choi"
        flavorText = "\"For every scar I've received, I've given ten back.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/7/17755d1b-3a56-4362-a534-85b35ceb1802.jpg?1783932628"
    }
}
