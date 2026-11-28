package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Curse of the Nightly Hunt
 * {2}{R}
 * Enchantment — Aura Curse
 * Enchant player
 * Creatures enchanted player controls attack each combat if able.
 *
 * A static [MustAttack] over the creatures controlled by whoever the Curse enchants
 * (`controlledByEnchantedPlayer()`), so it follows the enchanted player, not the Curse's controller.
 */
val CurseOfTheNightlyHunt = card("Curse of the Nightly Hunt") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura Curse"
    oracleText = "Enchant player\nCreatures enchanted player controls attack each combat if able."

    auraTarget = Targets.Player

    staticAbility {
        ability = MustAttack(GroupFilter(GameObjectFilter.Creature.controlledByEnchantedPlayer()))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "137"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49cbfcf2-462e-4bbf-a529-a70816eb1436.jpg?1783940940"
    }
}
