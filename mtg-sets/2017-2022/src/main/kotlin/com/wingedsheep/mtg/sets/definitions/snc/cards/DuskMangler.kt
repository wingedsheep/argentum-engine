package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dusk Mangler
 * {5}{B}{B}
 * Creature — Horror
 * 5/4
 * As an additional cost to cast this spell, sacrifice a creature, discard a card, or pay 4 life.
 * When this creature enters, each opponent sacrifices a creature of their choice, discards a card,
 * and loses 4 life.
 */
val DuskMangler = card("Dusk Mangler") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Horror"
    power = 5
    toughness = 4
    oracleText = "As an additional cost to cast this spell, sacrifice a creature, discard a card, or pay 4 life.\n" +
        "When this creature enters, each opponent sacrifices a creature of their choice, discards a card, and loses 4 life."

    additionalCost(Costs.additional.Choice(
        Costs.additional.SacrificePermanent(GameObjectFilter.Creature),
        Costs.additional.DiscardCards(1),
        Costs.additional.PayLife(4),
        choiceSlot = ChoiceSlot.ADDITIONAL_COST_BRANCH,
    ))

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.EachOpponentDiscards(1) then
            Effects.LoseLife(4, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "77"
        artist = "Antonio José Manzanedo"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b86825b2-5b03-4311-b7d9-f8131b17249d.jpg?1783923132"
    }
}
