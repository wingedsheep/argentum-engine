package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Homestead Courage
 * {W}
 * Sorcery
 * Put a +1/+1 counter on target creature you control. It gains vigilance until end of turn.
 * Flashback {W}
 */
val HomesteadCourage = card("Homestead Courage") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Put a +1/+1 counter on target creature you control. It gains vigilance until end of turn.\n" +
        "Flashback {W} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        val t = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t) then
            Effects.GrantKeyword(Keyword.VIGILANCE, t)
    }

    keywordAbility(KeywordAbility.flashback("{W}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "24"
        artist = "Colin Boyer"
        flavorText = "\"C'mon, fiend. I'll carve you up like a Harvesttide ham.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73a9c49f-fcd3-4572-bac7-6eb06fdc0815.jpg"
    }
}
