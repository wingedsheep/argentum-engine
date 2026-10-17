package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostZone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Corpse Explosion — {1}{B}{R}
 * Sorcery (SNC #179, rare)
 *
 * As an additional cost to cast this spell, exile a creature card from your graveyard.
 * Corpse Explosion deals damage equal to the exiled card's power to each creature and each
 * planeswalker.
 *
 * The exile additional cost records the card on the spell ([EffectTarget.ExiledAsCost]); at
 * resolution it is a card in exile, so its power reads its printed (or CDA) value.
 */
val CorpseExplosion = card("Corpse Explosion") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, exile a creature card from your graveyard.\n" +
        "Corpse Explosion deals damage equal to the exiled card's power to each creature and each planeswalker."

    additionalCost(
        Costs.additional.ExileCards(
            count = 1,
            filter = GameObjectFilter.Creature,
            fromZone = CostZone.GRAVEYARD
        )
    )

    spell {
        effect = Patterns.Group.dealDamageToAll(
            amount = DynamicAmounts.powerOf(EffectTarget.ExiledAsCost()),
            filter = GroupFilter(GameObjectFilter.CreatureOrPlaneswalker),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "179"
        artist = "Greg Staples"
        flavorText = "\"Well, that's one way to scatter her ashes.\"\n—Calvey, funeral director"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c700eff3-138b-4d4c-ba36-58b98986168c.jpg?1783923088"
    }
}
