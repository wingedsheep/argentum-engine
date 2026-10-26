package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Hazoret's Monument
 * {3}
 * Legendary Artifact
 * Red creature spells you cast cost {1} less to cast.
 * Whenever you cast a creature spell, you may discard a card. If you do, draw a card.
 *
 * The two lines read different sets of spells on purpose: only *red* creature spells are cheaper,
 * but the rummage trigger fires on any creature spell you cast.
 */
val HazoretsMonument = card("Hazoret's Monument") {
    manaCost = "{3}"
    typeLine = "Legendary Artifact"
    oracleText = "Red creature spells you cast cost {1} less to cast.\n" +
        "Whenever you cast a creature spell, you may discard a card. If you do, draw a card."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Creature.withColor(Color.RED)),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature)
        effect = Effects.May(
            Effects.IfYouDo(Patterns.Hand.discardCards(1), Effects.DrawCards(1))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "229"
        artist = "Richard Wright"
        flavorText = "\"The worthy shall rush to the God-Pharaoh's side with relentless zeal, rising to overcome every obstacle.\"\n—Monument inscription"
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a0a70f2-f2cb-4a08-a1a7-95c8fc3de6e3.jpg?1783936452"
    }
}
