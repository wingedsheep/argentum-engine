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
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5e134b3-e8af-41e9-928d-c217ea7b2b13.jpg"
        ruling(
            "10/4/2004",
            "This card was originally printed as a sorcery and has received errata to make it an instant."
        )
    }
}
