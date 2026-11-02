package com.wingedsheep.mtg.sets.definitions.m10.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Sleep
 * {2}{U}{U}
 * Sorcery
 * Tap all creatures target player controls. Those creatures don't untap during that player's next
 * untap step.
 *
 * "Those creatures" is every creature the player controls as Sleep resolves, tapped by it or not
 * (2018-07-13 ruling), so the untap lock is `lockedAtResolution`: fixed then, not re-matched at the
 * step against creatures they gain control of later.
 */
val Sleep = card("Sleep") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Tap all creatures target player controls. Those creatures don't untap during that player's next untap step."

    spell {
        val player = target(Targets.Player)
        effect = Patterns.Group.tapAll(GroupFilter(GameObjectFilter.Creature.targetPlayerControls(player))) then
            Effects.SkipUntap(player, GameObjectFilter.Creature, lockedAtResolution = true)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "71"
        artist = "Chris Rahn"
        flavorText = "\"I give them dreams so wondrous that they hesitate to return to the world of the conscious.\"\n—Garild, merfolk mage"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/133be4f4-1daa-41b6-b509-9e64c6b00059.jpg?1783942388"
        ruling("2018-07-13", "The second part of Sleep's ability affects all creatures the targeted player controls as Sleep resolves, not only the ones that Sleep actually caused to become tapped.")
    }
}
