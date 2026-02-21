package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PreventAllDamageDealtByTargetEffect

/**
 * Shieldmage Elder
 * {5}{W}
 * Creature — Human Cleric Wizard
 * 2/3
 * Tap two untapped Clerics you control: Prevent all damage target creature would deal this turn.
 * Tap two untapped Wizards you control: Prevent all damage target spell would deal this turn.
 */
val ShieldmageElder = card("Shieldmage Elder") {
    manaCost = "{5}{W}"
    typeLine = "Creature — Human Cleric Wizard"
    power = 2
    toughness = 3
    oracleText = "Tap two untapped Clerics you control: Prevent all damage target creature would deal this turn.\nTap two untapped Wizards you control: Prevent all damage target spell would deal this turn."

    activatedAbility {
        cost = Costs.TapPermanents(2, GameObjectFilter.Creature.withSubtype("Cleric"))
        val t = target("target", Targets.Creature)
        effect = PreventAllDamageDealtByTargetEffect(
            target = t
        )
    }

    activatedAbility {
        cost = Costs.TapPermanents(2, GameObjectFilter.Creature.withSubtype("Wizard"))
        val t = target("target", Targets.Spell)
        effect = PreventAllDamageDealtByTargetEffect(
            target = t
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "54"
        artist = "Matt Cavotta"
        imageUri = "https://cards.scryfall.io/normal/front/e/f/efa2d660-7c93-4087-a6e5-49c2ad21eb5a.jpg?1562951939"
    }
}
