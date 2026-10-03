package com.wingedsheep.mtg.sets.definitions.thb.cards

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
 * Dreadful Apathy
 * {2}{W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature can't attack or block.
 * {2}{W}: Exile enchanted creature.
 *
 * The Pacifism idiom ([CantAttack] + [CantBlock] over the attached creature) plus Sigarda's
 * Imprisonment's exile ability without the Blood token.
 */
val DreadfulApathy = card("Dreadful Apathy") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature can't attack or block.\n" +
        "{2}{W}: Exile enchanted creature."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    activatedAbility {
        cost = Costs.Mana("{2}{W}")
        effect = Effects.Exile(EffectTarget.EnchantedCreature)
        description = "{2}{W}: Exile enchanted creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "11"
        artist = "Mark Zug"
        flavorText = "Those whose lives were uninspired are doomed to the wretched tedium of Phylias in death."
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e20c2369-db77-465e-9f0e-bb009225345a.jpg?1783931599"
    }
}
