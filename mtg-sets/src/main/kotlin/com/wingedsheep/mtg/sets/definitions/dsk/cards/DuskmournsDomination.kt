package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ControlEnchantedPermanent
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Duskmourn's Domination
 * {4}{U}{U}
 * Enchantment — Aura
 * Enchant creature
 * You control enchanted creature.
 * Enchanted creature gets -3/-0 and loses all abilities.
 *
 * Three independent continuous effects from the Aura on its enchanted creature:
 *  - control change ([ControlEnchantedPermanent], Annex-style) — you become the creature's controller
 *    for as long as the Aura is attached;
 *  - a Layer 6 ability-removal ([LoseAllAbilities]); and
 *  - a Layer 7c stat modifier ([ModifyStats] -3/0). [ModifyStats] as an Aura staticAbility applies to
 *    the enchanted creature by default, so no explicit target is needed.
 */
val DuskmournsDomination = card("Duskmourn's Domination") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "You control enchanted creature.\n" +
        "Enchanted creature gets -3/-0 and loses all abilities."

    auraTarget = Targets.Creature

    // You control enchanted creature.
    staticAbility {
        ability = ControlEnchantedPermanent
    }

    // Enchanted creature loses all abilities.
    staticAbility {
        ability = LoseAllAbilities()
    }

    // Enchanted creature gets -3/-0.
    staticAbility {
        ability = ModifyStats(-3, 0)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "50"
        artist = "Eli Minaya"
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1d96dbc9-c2fd-42c1-9d55-5c14fa1c1c6f.jpg?1726286043"
    }
}
