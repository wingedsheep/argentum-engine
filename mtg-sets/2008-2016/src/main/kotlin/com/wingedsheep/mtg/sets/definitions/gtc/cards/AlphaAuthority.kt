package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Alpha Authority
 * {1}{G}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has hexproof and can't be blocked by more than one creature.
 *
 * The blocker cap is scoped to the enchanted creature (`Filters.EnchantedCreature`), which
 * `BlockPhaseManager` reads through its host-scoped `Scope.AttachedTo` path.
 */
val AlphaAuthority = card("Alpha Authority") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature has hexproof and can't be blocked by more than one creature."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantKeyword(Keyword.HEXPROOF, Filters.EnchantedCreature)
    }

    staticAbility {
        ability = CantBeBlockedByMoreThan(maxBlockers = 1, filter = Filters.EnchantedCreature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "114"
        artist = "Dave Kendall"
        flavorText = "Even the bitterest enemies of the Burning-Tree Clan respect the strength of Borborygmos's rule."
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fbc42ca6-db90-41d0-8a4b-3217fb2c114c.jpg?1783940119"
    }
}
