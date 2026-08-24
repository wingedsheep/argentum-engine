package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Icatian Skirmishers
 * {3}{W}
 * Creature — Human Soldier
 * 1/1
 * First strike; banding
 * Whenever this creature attacks, all creatures banded with it gain first strike until end of turn.
 *
 * "Banded with it" is Camel's filter — [GameObjectFilter.inSameBandAsSource], which only matches
 * while the source is attacking, so the band membership check needs no separate condition. The
 * Skirmishers themselves are excluded (`other()`): they already have first strike, and the oracle
 * text says the creatures banded *with* it.
 */
val IcatianSkirmishers = card("Icatian Skirmishers") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    oracleText = "First strike; banding (Any creatures with banding, and up to one without, can " +
        "attack in a band. Bands are blocked as a group. If any creatures with banding you " +
        "control are blocking or being blocked by a creature, you divide that creature's combat " +
        "damage, not its controller, among any of the creatures it's being blocked by or is " +
        "blocking.)\n" +
        "Whenever this creature attacks, all creatures banded with it gain first strike until end of turn."
    power = 1
    toughness = 1

    keywords(Keyword.FIRST_STRIKE, Keyword.BANDING)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Patterns.Group.grantKeywordToAll(
            Keyword.FIRST_STRIKE,
            GroupFilter(GameObjectFilter.Creature.inSameBandAsSource(), excludeSelf = true)
        )
        description = "Whenever this creature attacks, all creatures banded with it gain first strike until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "14"
        artist = "Heather Hudson"
        flavorText = "Skirmishers engaged raiders before they could reach the towns. Although these units typically suffered huge losses, they never lacked volunteers."
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15f6d115-c02d-45a3-aa6d-402964df47dd.jpg?1783947915"
    }
}
