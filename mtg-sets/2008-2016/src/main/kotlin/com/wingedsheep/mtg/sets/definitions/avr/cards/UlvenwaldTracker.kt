package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Ulvenwald Tracker
 * {G}
 * Creature — Human Shaman
 * 1 / 1
 *
 * {1}{G}, {T}: Target creature you control fights another target creature.
 *
 * Pit Fight's two-target fight on a tap ability: the second target is wrapped in [TargetOther]
 * so it may be another creature you control but not the first target (ruling).
 */
val UlvenwaldTracker = card("Ulvenwald Tracker") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Shaman"
    power = 1
    toughness = 1
    oracleText = "{1}{G}, {T}: Target creature you control fights another target creature."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{G}"),
            Costs.Tap
        )
        val mine = target(TargetFilter.CreatureYouControl)
        val other = target(TargetOther(TargetObject(filter = TargetFilter.Creature)))
        effect = Effects.Fight(mine, other)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "200"
        artist = "Christopher Moeller"
        flavorText = "\"Peace will come to Innistrad, but only after all abominations have been dealt with, one by one.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/6/46199391-a4f5-4532-b89c-b7691b229bd0.jpg?1783940658"
        ruling("2017-03-14", "The second target of Ulvenwald Tracker's ability can be another creature you control, but it can't be the same creature as the first target.")
        ruling("2017-03-14", "If either target of Ulvenwald Tracker's ability is an illegal target when the ability tries to resolve, neither creature will deal or be dealt damage.")
    }
}
