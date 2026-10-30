package com.wingedsheep.mtg.sets.definitions.lrw.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.TransformPermanent
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Lignify
 * {1}{G}
 * Kindred Enchantment — Treefolk Aura
 *
 * Enchant creature
 * Enchanted creature is a Treefolk with base power and toughness 0/4 and loses all abilities.
 *
 * Three layered statics over the enchanted creature, matching the printed clause order:
 *  - [TransformPermanent] with only `setCreatureSubtypes` — Layer 4 `SetCreatureSubtypes`
 *    **replaces** every creature type with Treefolk (the 2007-10-01 ruling: it overwrites earlier
 *    type-changing effects, changeling included). Card types and non-creature subtypes are
 *    untouched (CR 205.1a) — "is a Treefolk" changes only the creature type, so an artifact
 *    creature stays an artifact. Colors and name are untouched for the same reason.
 *  - [LoseAllAbilities] — Layer 6.
 *  - [SetBasePowerToughnessStatic] 0/4 — Layer 7b, so it overwrites a CDA (the ruling names
 *    Dauntless Dourbark) but loses to later +N/+N effects and counters, which apply in 7c/7d.
 *
 * Originally printed as a Tribal enchantment; "Tribal" was errata'd to "Kindred" in 2024 with no
 * gameplay change (2024-06-07 ruling).
 */
val Lignify = card("Lignify") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Kindred Enchantment — Treefolk Aura"
    oracleText = "Enchant creature\nEnchanted creature is a Treefolk with base power and " +
        "toughness 0/4 and loses all abilities."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = TransformPermanent(setCreatureSubtypes = setOf(Subtype.TREEFOLK.value))
    }

    staticAbility {
        ability = LoseAllAbilities()
    }

    staticAbility {
        ability = SetBasePowerToughnessStatic(0, 4)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "228"
        artist = "Jesper Ejsing"
        flavorText = "Bulgo paused, puzzled. What was that rustling sound, and why did he feel so " +
            "stiff? And how could his *feet* be so thirsty?"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/264fd6c5-8d73-4928-aed7-5ed637426780.jpg?1783942859"
        ruling("2007-10-01", "Lignify overwrites characteristic-defining abilities that specify " +
            "the enchanted creature's power or toughness (such as the one Dauntless Dourbark has) " +
            "or that specify its creature types (such as changeling does).")
        ruling("2009-10-01", "Lignify will not overwrite effects such as those from Glorious " +
            "Anthem, counters, or Giant Growth that change the enchanted creature's power or " +
            "toughness without setting it to a specific value. Apply such effects after applying " +
            "Lignify.")
    }
}
