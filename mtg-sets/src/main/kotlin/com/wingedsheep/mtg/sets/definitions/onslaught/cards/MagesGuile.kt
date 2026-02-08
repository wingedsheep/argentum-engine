package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Mage's Guile
 * {1}{U}
 * Instant
 * Target creature gains shroud until end of turn.
 * Cycling {U}
 */
val MagesGuile = card("Mage's Guile") {
    manaCost = "{1}{U}"
    typeLine = "Instant"

    spell {
        target = TargetCreature()
        effect = GrantKeywordUntilEndOfTurnEffect(Keyword.SHROUD, EffectTarget.ContextTarget(0))
    }

    keywordAbility(KeywordAbility.cycling("{U}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "91"
        artist = "Christopher Moeller"
        flavorText = "\"Next time, don't bother.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/0/301cb538-a931-4916-927b-4986046b1158.jpg?1562906277"
    }
}
