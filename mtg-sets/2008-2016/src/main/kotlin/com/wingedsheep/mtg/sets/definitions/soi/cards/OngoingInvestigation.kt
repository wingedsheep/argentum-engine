package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Ongoing Investigation (Shadows over Innistrad #77)
 * {1}{U}
 * Enchantment
 *
 * Whenever one or more creatures you control deal combat damage to a player, investigate. (Create a
 * Clue token. It's an artifact with "{2}, Sacrifice this token: Draw a card.")
 * {1}{G}, Exile a creature card from your graveyard: Investigate. You gain 2 life.
 */
val OngoingInvestigation = card("Ongoing Investigation") {
    manaCost = "{1}{U}"
    colorIdentity = "GU"
    typeLine = "Enchantment"
    oracleText = "Whenever one or more creatures you control deal combat damage to a player, investigate. " +
        "(Create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")\n" +
        "{1}{G}, Exile a creature card from your graveyard: Investigate. You gain 2 life."

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature).dealCombatDamageToAPlayer()
        effect = Effects.Investigate()
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{G}"),
            Costs.ExileFromGraveyard(1, GameObjectFilter.Creature)
        )
        effect = Effects.Investigate() then Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "77"
        artist = "Bud Cook"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19b8f962-8d4e-4dd8-a157-c0a55f9e152a.jpg"
    }
}
