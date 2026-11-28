package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Curse of Death's Hold
 * {3}{B}{B}
 * Enchantment — Aura Curse
 * Enchant player
 * Creatures enchanted player controls get -1/-1.
 *
 * A static [ModifyStats] over the creatures controlled by whoever the Curse enchants
 * (`controlledByEnchantedPlayer()`), so it follows the enchanted player, not the Curse's controller.
 */
val CurseOfDeathsHold = card("Curse of Death's Hold") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura Curse"
    oracleText = "Enchant player\nCreatures enchanted player controls get -1/-1."

    auraTarget = Targets.Player

    staticAbility {
        ability = ModifyStats(-1, -1, GroupFilter(GameObjectFilter.Creature.controlledByEnchantedPlayer()))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "94"
        artist = "Clint Cearley"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/1774d0a8-1cd3-4582-ace0-1caff92af0e7.jpg?1783940958"
    }
}
