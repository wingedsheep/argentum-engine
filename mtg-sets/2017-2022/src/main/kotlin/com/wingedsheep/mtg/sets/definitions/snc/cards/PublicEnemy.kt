package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.MustAttackDefender
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Public Enemy
 * {2}{U}
 * Enchantment — Aura
 * Enchant creature
 * All creatures attack enchanted creature's controller each combat if able.
 * When enchanted creature dies, draw a card.
 */
val PublicEnemy = card("Public Enemy") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "All creatures attack enchanted creature's controller each combat if able.\n" +
        "When enchanted creature dies, draw a card."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = MustAttack(GroupFilter.AllCreatures, defender = MustAttackDefender.EnchantedCreatureController)
    }

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "55"
        artist = "Dominik Mayer"
        flavorText = "Every Halo smuggler walks a razor's edge between untold riches and sudden disaster."
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e69ba61-8c16-4112-9601-fac978b88667.jpg?1783923141"
    }
}
