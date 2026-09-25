package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Cage of Hands
 * {2}{W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature can't attack or block.
 * {1}{W}: Return this Aura to its owner's hand.
 *
 * Pacifism's two statics plus Shackles' self-bounce. If the Aura has left the battlefield by the
 * time the bounce resolves it stays where it is (Scryfall ruling 2020-11-10) — the self move is
 * battlefield-sourced.
 */
val CageOfHands = card("Cage of Hands") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature can't attack or block.\n" +
        "{1}{W}: Return this Aura to its owner's hand."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    activatedAbility {
        cost = Costs.Mana("{1}{W}")
        effect = Effects.Move(EffectTarget.Self, Zone.HAND)
        description = "{1}{W}: Return this Aura to its owner's hand."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "3"
        artist = "Mark Tedin"
        flavorText = "\"Our own actions built the prisons that now hold us. Our hands reached too " +
            "far and tried to hold too much.\"\n—Dosan the Falling Leaf"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98a80b6a-90ed-482f-9714-eb856269e9d3.jpg?1783944342"
        ruling("2020-11-10", "The ability to return Cage of Hands to its owner's hand can only be activated if Cage of Hands is on the battlefield. If Cage of Hands is no longer on the battlefield when the ability resolves, Cage of Hands remains in its new zone and isn't returned to its owner's hand.")
    }
}
