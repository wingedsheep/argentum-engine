package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Pit Fight
 * {1}{R/G}
 * Instant
 * Target creature you control fights another target creature.
 *
 * Two single-target requirements, the second wrapped in [TargetOther] so it can't be the first
 * target — but it may be another creature you control (ruling).
 */
val PitFight = card("Pit Fight") {
    manaCost = "{1}{R/G}"
    colorIdentity = "RG"
    typeLine = "Instant"
    oracleText = "Target creature you control fights another target creature. (Each deals damage equal to its power to the other.)"

    spell {
        val mine = target(TargetFilter.CreatureYouControl)
        val other = target(TargetOther(TargetObject(filter = TargetFilter.Creature)))
        effect = Effects.Fight(mine, other)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "223"
        artist = "Matt Stewart"
        flavorText = "\"All the coins in Ravnica can't save his soul now.\"\n—Narbulg Nine Fingers"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/3624332e-60fc-4819-a0f1-d7ec41c6518b.jpg?1783940094"
        ruling("2013-01-24", "The second target of Pit Fight can be another creature you control, but it can't be the same creature as the first target.")
        ruling("2013-01-24", "If either target of Pit Fight is an illegal target when the ability tries to resolve, neither creature will deal or be dealt damage.")
    }
}
