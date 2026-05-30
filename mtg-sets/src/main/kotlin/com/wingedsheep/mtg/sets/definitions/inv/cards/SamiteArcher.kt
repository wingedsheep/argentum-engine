package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Samite Archer
 * {1}{W}{U}
 * Creature — Human Cleric Archer
 * 1/1
 * {T}: Prevent the next 1 damage that would be dealt to any target this turn.
 * {T}: This creature deals 1 damage to any target.
 */
val SamiteArcher = card("Samite Archer") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Human Cleric Archer"
    power = 1
    toughness = 1
    oracleText = "{T}: Prevent the next 1 damage that would be dealt to any target this turn.\n" +
        "{T}: This creature deals 1 damage to any target."

    activatedAbility {
        cost = Costs.Tap
        val t = target("target", AnyTarget())
        effect = Effects.PreventNextDamage(1, t)
        description = "{T}: Prevent the next 1 damage that would be dealt to any target this turn."
    }

    activatedAbility {
        cost = Costs.Tap
        val t = target("target", AnyTarget())
        effect = Effects.DealDamage(1, t)
        description = "{T}: This creature deals 1 damage to any target."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "269"
        artist = "Scott M. Fischer"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07a262d7-6d0c-43d0-89b6-9f46a1a9eb69.jpg?1562896515"
    }
}
