package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Lion Umbra
 * {G}{G}
 * Enchantment — Aura
 * Enchant modified creature
 * Enchanted creature gets +3/+3 and has reach and vigilance.
 * Umbra armor
 *
 * "Enchant modified creature" is the enchant restriction, so it's re-checked as a state-based
 * action: attached to your own creature, Lion Umbra keeps it modified by itself, but on a creature
 * another player controls it doesn't (CR 700.9 counts only Auras its controller controls) and falls
 * off unless something else modifies that creature.
 */
val LionUmbra = card("Lion Umbra") {
    manaCost = "{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant modified creature (Equipment, Auras its controller controls, and counters " +
        "are modifications.)\nEnchanted creature gets +3/+3 and has reach and vigilance.\n" +
        "Umbra armor (If enchanted creature would be destroyed, instead remove all damage from it " +
        "and destroy this Aura.)"

    keywords(Keyword.UMBRA_ARMOR)
    auraTarget = TargetObject(
        filter = TargetFilter(GameObjectFilter.Creature.withStatePredicate(StatePredicate.IsModified))
    )

    staticAbility {
        ability = ModifyStats(3, 3, GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = GrantKeyword(Keyword.REACH, GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter.attachedCreature())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "160"
        artist = "Julia Metzger"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89d58e9b-b1d9-4174-a30d-426d2e0ace07.jpg?1783911259"
    }
}
