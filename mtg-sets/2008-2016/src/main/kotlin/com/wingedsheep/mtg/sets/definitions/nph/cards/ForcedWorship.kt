package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Forced Worship
 * {1}{W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature can't attack.
 * {2}{W}: Return this Aura to its owner's hand.
 */
val ForcedWorship = card("Forced Worship") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText =
        "Enchant creature\n" +
        "Enchanted creature can't attack.\n" +
        "{2}{W}: Return this Aura to its owner's hand."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }

    activatedAbility {
        cost = Costs.Mana("{2}{W}")
        effect = Effects.Move(EffectTarget.Self, Zone.HAND)
        description = "{2}{W}: Return this Aura to its owner's hand."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "11"
        artist = "Karl Kopinski"
        flavorText = "Imprisonment teaches revenge. Hobbling teaches resignation."
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e050701d-4609-470d-85ff-4b7638893c6a.jpg?1783941326"
        ruling(
            "2011-06-01",
            "Forced Worship's activated ability may only be activated if Forced Worship is on the battlefield. " +
                "If it's no longer on the battlefield when the ability resolves, the ability has no effect.",
        )
    }
}
