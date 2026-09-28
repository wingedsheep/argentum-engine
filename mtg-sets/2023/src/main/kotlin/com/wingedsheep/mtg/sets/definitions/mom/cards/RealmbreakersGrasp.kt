package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Realmbreaker's Grasp
 * {1}{W}
 * Enchantment — Aura
 *
 * Enchant artifact or creature
 * Enchanted permanent can't attack or block, and its activated abilities can't be
 * activated unless they're mana abilities.
 *
 * Petrify's artifact-or-creature lock with Faith's Fetters' mana-ability carve-out:
 * [PreventActivatedAbilities] scoped to the host with `nonManaAbilitiesOnly = true`.
 * [GroupFilter.attachedCreature] is a scope-based `Permanent` filter, so an artifact host
 * is covered too (and the combat halves bite if it later animates).
 */
val RealmbreakersGrasp = card("Realmbreaker's Grasp") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant artifact or creature\n" +
        "Enchanted permanent can't attack or block, and its activated abilities can't be " +
        "activated unless they're mana abilities."

    auraTarget = TargetObject(filter = TargetFilter.CreatureOrArtifact)

    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = PreventActivatedAbilities(
            filter = GameObjectFilter.Permanent.attachedToBySource(),
            nonManaAbilitiesOnly = true,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "33"
        artist = "Artur Nakhodkin"
        flavorText = "The Invasion Tree wasn't merely a conduit through which to breach planes. " +
            "It was a living, writhing extension of Phyrexia's will."
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67a41675-47dd-40ca-a30c-0fd0faa32b76.jpg?1783917053"
    }
}
