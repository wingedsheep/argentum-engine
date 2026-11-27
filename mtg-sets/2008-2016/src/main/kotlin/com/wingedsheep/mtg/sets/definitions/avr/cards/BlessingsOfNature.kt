package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Blessings of Nature
 * {4}{G}
 * Sorcery
 *
 * Distribute four +1/+1 counters among any number of target creatures.
 * Miracle {G} (You may cast this card for its miracle cost when you draw it if it's the first card you drew this turn.)
 *
 * Defend the Celestus's distribution shape: "any number" of targets is capped at four because each
 * target must receive at least one counter, and the split is announced as the spell is cast. At
 * least one target is required — there is nothing to distribute four counters among otherwise.
 */
val BlessingsOfNature = card("Blessings of Nature") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Distribute four +1/+1 counters among any number of target creatures.\n" +
        "Miracle {G} (You may cast this card for its miracle cost when you draw it if it's the " +
        "first card you drew this turn.)"

    spell {
        targets(TargetFilter.Creature, count = 4, minCount = 1)
        effect = Effects.DistributeCountersAmongTargets(totalCounters = 4)
    }

    keywordAbility(KeywordAbility.miracle("{G}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "168"
        artist = "Anthony Francisco"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d16453f1-c6ca-4288-ab72-315ed9bb0ab0.jpg?1783940671"
    }
}
