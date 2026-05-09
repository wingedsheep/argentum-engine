package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

val EarthbendingLesson = card("Earthbending Lesson") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery — Lesson"
    oracleText = "Earthbend 4. (Target land you control becomes a 0/0 creature with haste that's still a land. Put four +1/+1 counters on it. When it dies or is exiled, return it to the battlefield tapped.)"

    spell {
        val t = target("target land you control", TargetObject(filter = TargetFilter.Land.youControl()))
        effect = Effects.Earthbend(4, t)
    }

    keywordAbility(KeywordAbility.Numeric(Keyword.EARTHBEND, 4))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "176"
        artist = "Toni Infante"
        flavorText = "\"Rock is a stubborn element. If you're going to move it, you've got to be like a rock yourself.\"\n—Toph"
        imageUri = "https://cards.scryfall.io/normal/front/e/c/eccd63b3-3a3a-4661-9d6e-fb8152429bdb.jpg?1764121195"
    }
}
