package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Syr Elenora, the Discerning
 * {3}{U}{U}
 * Legendary Creature — Human Knight
 * * /4
 *
 * Syr Elenora's power is equal to the number of cards in your hand.
 * When Syr Elenora enters, draw a card.
 * Spells your opponents cast that target Syr Elenora cost {2} more to cast.
 *
 * The power is a characteristic-defining ability (CR 604.3), so it is the `dynamicPower` slot and
 * works in every zone, per the ruling.
 */
val SyrElenoraTheDiscerning = card("Syr Elenora, the Discerning") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Knight"
    oracleText = "Syr Elenora's power is equal to the number of cards in your hand.\n" +
        "When Syr Elenora enters, draw a card.\n" +
        "Spells your opponents cast that target Syr Elenora cost {2} more to cast."
    toughness = 4

    dynamicPower(DynamicAmounts.cardsInYourHand())

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.OpponentsCastTargeting(GroupFilter.source()),
            modification = CostModification.IncreaseGeneric(2),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "67"
        artist = "Mila Pesic"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/050a0817-2e9c-4d98-974c-2d3e5c37e1a2.jpg?1783932648"

        ruling("2019-10-04", "The ability that defines Syr Elenora's power works in all zones, not just the battlefield.")
        ruling("2019-10-04", "To determine the total cost of an opponent's spell that targets Syr Elenora, start with the mana cost or alternative cost that player is paying, add any cost increases (such as that of Syr Elenora's effect), then apply any cost reductions. The mana value of the spell remains unchanged, no matter what the total cost to cast it was.")
    }
}
