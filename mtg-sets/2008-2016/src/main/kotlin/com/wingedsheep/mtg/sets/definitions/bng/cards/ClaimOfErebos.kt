package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Claim of Erebos
 * {1}{B}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature has "{1}{B}, {T}: Target player loses 2 life."
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val ClaimOfErebos = card("Claim of Erebos") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature has \"{1}{B}, {T}: Target player loses 2 life.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.Tap)
                val player = target(Targets.Player)
                effect = Effects.LoseLife(2, player)
            }
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "64"
        artist = "Zack Stella"
        flavorText = "Priests of Erebos give an occasional reminder to mortals who have forgotten their mortality."
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a51db556-b3e8-4628-9e39-6353023b0696.jpg?1783939559"
    }
}
