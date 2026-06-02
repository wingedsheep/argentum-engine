package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.GroupPatterns

/**
 * Mouth of the Storm
 * {6}{U}
 * Creature — Elemental
 * Flying
 * Ward {2} (Whenever this creature becomes the target of a spell or ability an opponent controls, counter it unless that player pays {2}.)
 * When this creature enters, creatures your opponents control get -3/-0 until your next turn.
 * 6/6
 */
val MouthOfTheStorm = card("Mouth of the Storm") {
    manaCost = "{6}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental"
    power = 6
    toughness = 6
    oracleText = "Flying\nWard {2} (Whenever this creature becomes the target of a spell or ability an opponent controls, counter it unless that player pays {2}.)\nWhen this creature enters, creatures your opponents control get -3/-0 until your next turn."

    keywords(Keyword.FLYING, Keyword.WARD)
    keywordAbility(KeywordAbility.ward("{2}"))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = GroupPatterns.modifyStatsForAll(
            power = -3,
            toughness = 0,
            filter = GroupFilter.AllCreaturesOpponentsControl,
            duration = Duration.UntilYourNextTurn
        )
        description = "When this creature enters, creatures your opponents control get -3/-0 until your next turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "70"
        artist = "Domenico Cava"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/380f16d6-ad43-4e0d-9645-6abde6248182.jpg?1752946834"
    }
}
