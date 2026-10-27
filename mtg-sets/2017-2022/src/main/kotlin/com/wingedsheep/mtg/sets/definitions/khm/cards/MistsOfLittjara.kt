package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Mists of Littjara — Kaldheim #67
 * {1}{U} · Enchantment — Aura
 *
 * Flash
 * Enchant creature or Vehicle
 * Enchanted creature gets -3/-0.
 *
 * The enchant clause is [GameObjectFilter.CreatureOrVehicle], as on Aerial Modification. The -3/-0
 * is Short Circuit's shape: gated by [Conditions.EnchantedPermanentMatches] on the host's current
 * (projected) type, so an uncrewed Vehicle is unaffected until it becomes a creature.
 */
val MistsOfLittjara = card("Mists of Littjara") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant creature or Vehicle\n" +
        "Enchanted creature gets -3/-0."

    keywords(Keyword.FLASH)

    auraTarget = TargetObject(filter = TargetFilter(GameObjectFilter.CreatureOrVehicle))

    staticAbility {
        condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Creature)
        ability = ModifyStats(-3, 0, GroupFilter.attachedCreature())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "67"
        artist = "Bastien L. Deharme"
        flavorText = "The Skelle raiders boasted that nothing could stop them, certainly not a bit of fog."
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6cfd4944-790f-4835-a8a1-c23ef7047648.jpg?1783928259"
    }
}
