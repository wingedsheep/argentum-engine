package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val WithoutWeakness = card("Without Weakness") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature you control gains indestructible until end of turn. " +
        "(Damage and effects that say \"destroy\" don't destroy it. If its toughness is 0 or less, it still dies.)\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)"

    spell {
        val t = target(TargetFilter.CreatureYouControl)
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, t)
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "81"
        artist = "Alex Konstad"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6695fa8-881c-407c-91d9-3ac770372d35.jpg?1783936035"
    }
}
