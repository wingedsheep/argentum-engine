package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Spectral Cloak
 * {U}{U}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has shroud as long as it's untapped.
 *
 * The shroud grant is gated on the enchanted creature's live tapped status, so it switches off the
 * moment the creature taps (e.g. to attack) and back on when it untaps. Gaining shroud doesn't
 * remove Auras already attached — including this one (per the 2005-08-01 ruling).
 */
val SpectralCloak = card("Spectral Cloak") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature has shroud as long as it's untapped. " +
        "(It can't be the target of spells or abilities.)"
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Any.untapped())
        ability = GrantKeyword(Keyword.SHROUD)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "78"
        artist = "Rob Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/7524fd0d-a675-41d6-bc99-bd3ba336893b.jpg?1783948071"
        ruling("2005-08-01", "Does not destroy Auras already on the creature.")
    }
}
