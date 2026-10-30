package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.TransformPermanent
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Ichthyomorphosis
 * {3}{U}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature loses all abilities and is a blue Fish with base power and toughness 0/1.
 *
 * "Is a blue Fish" names a creature type but no card type, so per CR 205.1a it replaces only the
 * creature types: an enchantment creature stays an enchantment, a legendary one stays legendary
 * (`setCreatureSubtypes`, not `setCardTypes` + `setSubtypes`). Colors are overwritten to blue
 * (Layer 5), abilities removed (Layer 6), base P/T set to 0/1 (Layer 7b).
 */
val Ichthyomorphosis = card("Ichthyomorphosis") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature loses all abilities and is a blue Fish with " +
        "base power and toughness 0/1."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = LoseAllAbilities()
    }

    staticAbility {
        ability = TransformPermanent(setCreatureSubtypes = setOf("Fish"), setColors = setOf(Color.BLUE))
    }

    staticAbility {
        ability = SetBasePowerToughnessStatic(0, 1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "51"
        artist = "Campbell White"
        flavorText = "Tycthis's military career was a flop."
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4763b6c-6471-41bd-b3e4-785564bcf06f.jpg?1783931583"
        ruling("2020-01-24", "Ichthyomorphosis overwrites all colors and creature types the enchanted " +
            "creature has. It's just a blue Fish. The creature keeps any supertypes (such as legendary) " +
            "it has, as well as any other card types it has (such as enchantment).")
    }
}
