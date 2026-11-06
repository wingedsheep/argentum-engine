package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Ice Over — Aether Revolt #35 (canonical printing)
 * {1}{U} · Enchantment — Aura
 *
 * Enchant artifact or creature
 * Enchanted permanent doesn't untap during its controller's untap step.
 *
 * Inertia Bubble's shape widened to artifact or creature: the bare [GrantKeyword] static scopes to
 * the attached permanent. It doesn't tap on entry.
 */
val IceOver = card("Ice Over") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant artifact or creature\n" +
        "Enchanted permanent doesn't untap during its controller's untap step."

    auraTarget = TargetObject(filter = TargetFilter.CreatureOrArtifact)

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "35"
        artist = "Sidharth Chaturvedi"
        flavorText = "\"Slow the Consulate with whatever you've got!\"\n—Jasi Ganin, master siphoner"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c01d2835-060c-4ac3-b586-84811878a64d.jpg?1783936773"
        ruling("2017-02-09", "Ice Over may target and may enchant an untapped creature.")
    }
}
