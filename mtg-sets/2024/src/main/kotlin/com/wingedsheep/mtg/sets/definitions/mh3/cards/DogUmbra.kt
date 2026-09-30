package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Dog Umbra
 * {1}{W}
 * Enchantment — Aura
 * Flash
 * Enchant creature
 * As long as another player controls enchanted creature, it can't attack or block. Otherwise, this
 * Aura has umbra armor.
 *
 * "Another player" is relative to the Aura's controller — the "you" of an Aura's condition — and
 * both controllers are read through projection, so the two halves swap the moment either permanent
 * changes hands (per the rulings). The umbra armor half is a projected self-grant of
 * [Keyword.UMBRA_ARMOR], which the engine reads off the Aura at every destruction.
 */
private val enchantedByAnotherPlayer = Conditions.EnchantedPermanentMatches(
    GameObjectFilter.Creature.withControllerPredicate(
        ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
    )
)

val DogUmbra = card("Dog Umbra") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\nEnchant creature\n" +
        "As long as another player controls enchanted creature, it can't attack or block. " +
        "Otherwise, this Aura has umbra armor. (If enchanted creature would be destroyed, instead " +
        "remove all damage from it and destroy this Aura.)"

    keywords(Keyword.FLASH)
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
        condition = enchantedByAnotherPlayer
    }

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
        condition = enchantedByAnotherPlayer
    }

    staticAbility {
        ability = GrantKeyword(Keyword.UMBRA_ARMOR, GroupFilter.source())
        condition = Conditions.Not(enchantedByAnotherPlayer)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "22"
        artist = "Brian Valeza"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d4ba710-eddb-40ca-b2fe-0e4e778aab9c.jpg?1783911304"
    }
}
