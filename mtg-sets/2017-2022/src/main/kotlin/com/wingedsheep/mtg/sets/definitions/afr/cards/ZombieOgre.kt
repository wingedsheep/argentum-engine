package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Zombie Ogre
 * {3}{B}{B}
 * Creature — Zombie Ogre
 * 3/5
 * At the beginning of your end step, if a creature died this turn, venture into the dungeon.
 * (Enter the first room or advance to the next room.)
 *
 * The death check is an intervening-if (CR 603.4): if no creature has died as the end step
 * begins, the ability doesn't trigger at all.
 */
val ZombieOgre = card("Zombie Ogre") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Ogre"
    power = 3
    toughness = 5
    oracleText = "At the beginning of your end step, if a creature died this turn, venture into the dungeon. " +
        "(Enter the first room or advance to the next room.)"

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.CreatureDiedThisTurn
        effect = Effects.VentureIntoTheDungeon()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "129"
        artist = "Nicholas Gregory"
        flavorText = "\"On the plus side, Throg no longer thinks he's clever.\"\n—Xanathar"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4dbed36-190c-4748-b282-409a2fb5d134.jpg?1783926485"
        ruling("2021-07-23", "Zombie Ogre doesn't need to have been on the battlefield at the time the creature died for its ability to function.")
        ruling("2021-07-23", "Zombie Ogre's ability will trigger only once during your end step, no matter how many creatures died this turn. However, if no creatures have died so far this turn as the end step begins, the ability won't trigger at all. It's not possible to cause a creature to die during the end step in time to have the ability trigger.")
    }
}
