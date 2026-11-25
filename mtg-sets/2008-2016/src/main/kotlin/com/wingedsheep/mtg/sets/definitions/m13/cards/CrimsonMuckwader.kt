package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Crimson Muckwader
 * {1}{R}
 * Creature — Lizard
 * 2/1
 * This creature gets +1/+1 as long as you control a Swamp.
 * {2}{B}: Regenerate this creature.
 */
val CrimsonMuckwader = card("Crimson Muckwader") {
    manaCost = "{1}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Lizard"
    oracleText = "This creature gets +1/+1 as long as you control a Swamp.\n" +
        "{2}{B}: Regenerate this creature. (The next time this creature would be destroyed this turn, instead tap it, remove it from combat, and heal all damage on it.)"
    power = 2
    toughness = 1

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(1, 1, Filters.Self),
            condition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Swamp")),
        )
    }

    activatedAbility {
        cost = Costs.Mana("{2}{B}")
        effect = Effects.Regenerate(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "127"
        artist = "Steven Belledin"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a0811f91-ed92-4a8e-badd-ae5054e7707d.jpg"
    }
}
