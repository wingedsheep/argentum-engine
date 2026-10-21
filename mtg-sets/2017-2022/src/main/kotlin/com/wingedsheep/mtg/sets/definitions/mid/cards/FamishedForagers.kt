package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Famished Foragers
 * {3}{R}
 * Creature — Vampire
 * 4/3
 * When this creature enters, if an opponent lost life this turn, add {R}{R}{R}.
 * {2}{R}, Discard a card: Draw a card.
 *
 * Intervening-if ETB ([Conditions.OpponentLostLifeThisTurn], CR 603.4) adding three red mana,
 * plus a mana-and-discard rummage ability.
 */
val FamishedForagers = card("Famished Foragers") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Vampire"
    power = 4
    toughness = 3
    oracleText = "When this creature enters, if an opponent lost life this turn, add {R}{R}{R}.\n" +
        "{2}{R}, Discard a card: Draw a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.OpponentLostLifeThisTurn
        effect = Effects.AddMana(Color.RED, 3)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{R}"), Costs.DiscardCard)
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "138"
        artist = "Lorenzo Mastroianni"
        flavorText = "Furious to find the workshop empty, the starving vampires flew into a frenzy."
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fdbe6937-ceda-4367-a101-562fd5e0e09d.jpg"
    }
}
