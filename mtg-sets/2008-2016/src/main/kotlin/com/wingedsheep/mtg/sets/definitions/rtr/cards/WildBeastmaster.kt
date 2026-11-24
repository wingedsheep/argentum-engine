package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Wild Beastmaster
 * {2}{G}
 * Creature — Human Shaman
 * 1/1
 * Whenever this creature attacks, each other creature you control gets +X/+X until end of turn,
 * where X is this creature's power.
 *
 * X is read as the trigger resolves (last-known power if it has left the battlefield) and locked
 * into the end-of-turn pump; a negative power counts as 0.
 */
val WildBeastmaster = card("Wild Beastmaster") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Shaman"
    oracleText = "Whenever this creature attacks, each other creature you control gets +X/+X until end of turn, " +
        "where X is this creature's power."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Patterns.Group.modifyStatsForAll(
            power = DynamicAmounts.nonNegative(DynamicAmounts.sourcePower()),
            toughness = DynamicAmounts.nonNegative(DynamicAmounts.sourcePower()),
            filter = GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "139"
        artist = "Kev Walker"
        flavorText = "\"When they covered the world in city, all they did was give themselves nowhere to run.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4d4ef98-949b-49db-b4f9-a070f8b4ff47.jpg?1783940345"
        ruling("2017-07-14", "If this creature's power is negative as its ability resolves, X is considered to be 0.")
        ruling("2012-10-01", "The value of X is determined when the triggered ability resolves. If Wild Beastmaster is no longer on the battlefield at that time, use its last known power to determine the value of X.")
    }
}
