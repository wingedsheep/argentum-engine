package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Regal Leosaur
 * {R}{W}
 * Creature — Dinosaur Cat
 * 2/2
 * Mutate {1}{R/W}{R/W}
 * Whenever this creature mutates, other creatures you control get +2/+1 until end of turn.
 */
val RegalLeosaur = card("Regal Leosaur") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Dinosaur Cat"
    power = 2
    toughness = 2
    oracleText = "Mutate {1}{R/W}{R/W} (If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it.)\n" +
        "Whenever this creature mutates, other creatures you control get +2/+1 until end of turn."

    keywordAbility(KeywordAbility.mutate("{1}{R/W}{R/W}"))

    triggeredAbility {
        trigger = Triggers.self.mutates()
        effect = Patterns.Group.modifyStatsForAll(2, 1, GroupFilter.OtherCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "202"
        artist = "Ilse Gort"
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b6fdf313-740b-4976-911f-9fb5eb54afce.jpg?1783931018"
    }
}
