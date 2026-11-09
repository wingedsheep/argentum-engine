package com.wingedsheep.mtg.sets.definitions.tor.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Ghostly Wings (Torment #38)
 * {1}{U}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets +1/+1 and has flying.
 * Discard a card: Return enchanted creature to its owner's hand.
 */
val GhostlyWings = card("Ghostly Wings") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +1/+1 and has flying.\n" +
        "Discard a card: Return enchanted creature to its owner's hand."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING)
    }

    activatedAbility {
        cost = Costs.DiscardCard
        effect = Effects.ReturnToHand(EffectTarget.EnchantedCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "38"
        artist = "David Martin"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58fb2705-82e7-49d9-b8cc-f98f652dd6c1.jpg"
    }
}
