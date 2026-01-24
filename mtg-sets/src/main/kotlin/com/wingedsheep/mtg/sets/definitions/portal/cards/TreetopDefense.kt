package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CreatureGroupFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToGroupEffect
import com.wingedsheep.sdk.scripting.YouWereAttackedThisStep

/**
 * Treetop Defense
 * {1}{G}
 * Instant
 * Cast this spell only during the declare attackers step and only if you've been attacked this step.
 * Creatures you control gain reach until end of turn.
 */
val TreetopDefense = card("Treetop Defense") {
    manaCost = "{1}{G}"
    typeLine = "Instant"

    spell {
        castOnlyDuring(Step.DECLARE_ATTACKERS)
        castOnlyIf(YouWereAttackedThisStep)
        effect = GrantKeywordToGroupEffect(
            keyword = Keyword.REACH,
            filter = CreatureGroupFilter.AllYouControl
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "190"
        artist = "Richard Kane Ferguson"
        flavorText = "From the treetops, no flyer escapes unscathed."
        imageUri = "https://cards.scryfall.io/normal/front/5/1/51a8b3b6-8f49-475f-b4fc-6e19cb6c6726.jpg"
    }
}
