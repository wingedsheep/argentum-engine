package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Meteor Swarm — Adventures in the Forgotten Realms #155
 * {X}{R}{R}{R} · Sorcery
 *
 * Meteor Swarm deals 8 damage divided as you choose among X target creatures and/or planeswalkers.
 *
 * Exactly X targets (`exactly = xValue()`), with the 8 damage divided among them as the spell is
 * cast (CR 601.2d). Each target must be assigned at least 1 damage, so the printed ruling caps X
 * at 8; `xMaximum` makes that cap visible as X is announced, so the X picker never offers a ninth
 * target the division could not cover.
 */
val MeteorSwarm = card("Meteor Swarm") {
    manaCost = "{X}{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Meteor Swarm deals 8 damage divided as you choose among X target creatures and/or planeswalkers."

    spell {
        xMaximum = DynamicAmounts.fixed(8)
        targets(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker), exactly = DynamicAmounts.xValue())
        effect = Effects.DividedDamage(total = 8, minTargets = 0, maxTargets = 8)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "155"
        artist = "Olivier Bernard"
        flavorText = "Sometimes one fireball isn't enough."
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b8f5e669-4e69-4763-bbb9-e9685bde912c.jpg?1783926474"
        ruling("2021-07-23", "You divide the damage as you cast Meteor Swarm. Each target must be assigned at least 1 damage. Because there is a maximum of eight targets, X can't be greater than 8.")
        ruling("2021-07-23", "If Meteor Swarm has one or more targets, and some (but not all) of them are illegal as Meteor Swarm tries to resolve, the amount of damage dealt to the remaining legal targets doesn't change. If all targets become illegal, Meteor Swarm won't resolve.")
    }
}
