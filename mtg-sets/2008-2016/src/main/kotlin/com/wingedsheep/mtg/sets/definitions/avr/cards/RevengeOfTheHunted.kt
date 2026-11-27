package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Revenge of the Hunted
 * {4}{G}{G}
 * Sorcery
 *
 * Until end of turn, target creature gets +6/+6 and gains trample, and all creatures able to block
 * it this turn do so.
 * Miracle {G} (You may cast this card for its miracle cost when you draw it if it's the first card
 * you drew this turn.)
 */
val RevengeOfTheHunted = card("Revenge of the Hunted") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Until end of turn, target creature gets +6/+6 and gains trample, and all creatures " +
        "able to block it this turn do so.\n" +
        "Miracle {G} (You may cast this card for its miracle cost when you draw it if it's the " +
        "first card you drew this turn.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(6, 6, creature) then
            Effects.GrantKeyword(Keyword.TRAMPLE, creature) then
            Effects.MustBeBlocked(creature)
    }

    keywordAbility(KeywordAbility.miracle("{G}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "191"
        artist = "Christopher Moeller"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36f7d663-115c-4ad0-a072-633df054cce4.jpg?1783940662"
    }
}
