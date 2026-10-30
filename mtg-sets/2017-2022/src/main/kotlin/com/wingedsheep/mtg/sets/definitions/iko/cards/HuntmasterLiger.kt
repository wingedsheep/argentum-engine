package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/**
 * Huntmaster Liger
 * {3}{W}
 * Creature — Cat
 * 3/4
 * Mutate {2}{W}
 * Whenever this creature mutates, other creatures you control get +X/+X until end of turn,
 * where X is the number of times this creature has mutated.
 */
val HuntmasterLiger = card("Huntmaster Liger") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat"
    power = 3
    toughness = 4
    oracleText = "Mutate {2}{W} (If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it.)\n" +
        "Whenever this creature mutates, other creatures you control get +X/+X until end of turn, where X is the number of times this creature has mutated."

    keywordAbility(KeywordAbility.mutate("{2}{W}"))

    triggeredAbility {
        trigger = Triggers.self.mutates()
        val timesMutated = DynamicAmounts.propertyOf(EffectTarget.Self, EntityNumericProperty.TimesMutated)
        effect = Patterns.Group.modifyStatsForAll(timesMutated, timesMutated, GroupFilter.OtherCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "16"
        artist = "Leesha Hannigan"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/058d6a7d-12b0-4d82-a5bd-91fa44bbc5e1.jpg"
    }
}
