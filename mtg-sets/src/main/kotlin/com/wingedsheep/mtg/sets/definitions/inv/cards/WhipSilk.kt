package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects

/**
 * Whip Silk
 * {G}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has reach.
 * {G}: Return this Aura to its owner's hand.
 */
val WhipSilk = card("Whip Silk") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature has reach. (It can block creatures with flying.)\n" +
        "{G}: Return this Aura to its owner's hand."

    auraTarget = Targets.Creature

    staticAbility {
        ability = GrantKeyword(Keyword.REACH)
    }

    activatedAbility {
        cost = Costs.Mana("{G}")
        effect = Effects.Move(EffectTarget.Self, Zone.HAND)
        description = "{G}: Return this Aura to its owner's hand."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "225"
        artist = "Dave Dorman"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/10566804-fd15-4ef0-ad7d-cc979f4cc8c5.jpg?1562898296"
    }
}
