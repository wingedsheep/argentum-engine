package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.TransformPermanent
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Amphibian Downpour — Modern Horizons 3 #51 (rare)
 * {2}{U} · Enchantment — Aura
 *
 * Flash
 * Storm (When you cast this spell, copy it for each spell cast before it this turn. You may choose
 * new targets for the copies. Copies become tokens.)
 * Enchant creature
 * Enchanted creature loses all abilities and is a blue Frog creature with base power and
 * toughness 1/1.
 *
 * Each storm copy is an Aura spell of its own: the enchant target is one of the spell's targets, so
 * every copy may pick a new creature, and it resolves into a token Aura attached to it (CR 707.10f).
 */
val AmphibianDownpour = card("Amphibian Downpour") {
    manaCost = "{2}{U}"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\nStorm (When you cast this spell, copy it for each spell cast before it this turn. " +
        "You may choose new targets for the copies. Copies become tokens.)\nEnchant creature\n" +
        "Enchanted creature loses all abilities and is a blue Frog creature with base power and toughness 1/1."

    keywords(Keyword.FLASH, Keyword.STORM)

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    // "loses all abilities" — Layer 6
    staticAbility {
        ability = LoseAllAbilities()
    }

    // "is a blue Frog creature" — Layer 4 (card types + subtypes replaced) and Layer 5 (colour)
    staticAbility {
        ability = TransformPermanent(
            setCardTypes = setOf("CREATURE"),
            setSubtypes = setOf("Frog"),
            setColors = setOf(Color.BLUE)
        )
    }

    // "base power and toughness 1/1" — Layer 7b
    staticAbility {
        ability = SetBasePowerToughnessStatic(1, 1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "51"
        artist = "Omar Rayyan"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d8aeca5-622a-45be-8168-07e7c00e3092.jpg?1783911294"
    }
}
