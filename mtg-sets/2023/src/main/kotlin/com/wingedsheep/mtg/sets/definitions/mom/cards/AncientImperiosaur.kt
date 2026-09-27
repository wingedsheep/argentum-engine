package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost

/**
 * Ancient Imperiosaur
 * {5}{G}{G}
 * Creature — Dinosaur
 * 6/6
 * Convoke
 * Trample, ward {2}
 * This creature enters with two +1/+1 counters on it for each creature that convoked it.
 *
 * "Each creature that convoked it" is the convoke record the spell carries onto the permanent
 * (CR 702.51c), counted by [DynamicAmounts.convokedCreatureCount]. Modelled as an
 * [EntersWithDynamicCounters] replacement (CR 614.1c) so the counters are there as it enters.
 */
val AncientImperiosaur = card("Ancient Imperiosaur") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dinosaur"
    power = 6
    toughness = 6
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while " +
        "casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "Trample, ward {2}\n" +
        "This creature enters with two +1/+1 counters on it for each creature that convoked it."

    keywords(Keyword.CONVOKE, Keyword.TRAMPLE)
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))
    replacementEffect(EntersWithDynamicCounters(count = DynamicAmounts.convokedCreatureCount() * 2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "174"
        artist = "Piotr Foksowicz"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/687d3261-dfbf-4c49-986f-20117b7ab5e7.jpg?1783916978"
        ruling(
            "2023-04-14",
            "You can't tap more creatures to convoke Ancient Imperiosaur than is necessary to pay for the spell. In most cases, this means seven creatures. However, if there are any additional costs to cast Ancient Imperiosaur, you may use convoke to pay those additional costs as well."
        )
    }
}
