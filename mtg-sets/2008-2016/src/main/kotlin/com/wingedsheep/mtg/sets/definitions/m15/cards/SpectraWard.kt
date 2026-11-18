package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantProtection
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.ProtectionSpares
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Spectra Ward — Magic 2015 #36 (canonical printing)
 * {3}{W}{W} · Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets +2/+2 and has protection from each color. This effect doesn't remove
 * Auras.
 *
 * Protection from each color is one [GrantProtection] per color (as Favor of the Mighty), each
 * sparing every Aura ([ProtectionSpares.AURAS]): Auras already on the creature stay, but colored
 * Aura spells still can't target it, and protection from a color gained another way still removes
 * Auras of that color — Spectra Ward included.
 */
val SpectraWard = card("Spectra Ward") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +2/+2 and has protection from each color. " +
        "This effect doesn't remove Auras. (It can't be blocked, targeted, or dealt damage by anything " +
        "that's white, blue, black, red, or green.)"
    auraTarget = TargetObject(filter = TargetFilter.Creature)
    staticAbility {
        ability = ModifyStats(2, 2)
    }
    for (color in Color.entries) {
        staticAbility {
            ability = GrantProtection(color, spares = ProtectionSpares.AURAS)
        }
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "36"
        artist = "Ryan Alexander Lee"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69c1b7a2-a4ff-485c-9fe5-04c88e5e8886.jpg?1783939198"
        ruling("2014-07-18", "The protection granted by Spectra Ward won't cause any Aura to be put into its owner's graveyard, including Spectra Ward itself. However, if the enchanted creature gains protection from white in another way, Spectra Ward will be put into its owner's graveyard as a state-based action.")
        ruling("2014-07-18", "Although Auras that are already attached to the creature aren't affected by Spectra Ward, the enchanted creature can't be the target of further Aura spells that have one or more colors.")
    }
}
