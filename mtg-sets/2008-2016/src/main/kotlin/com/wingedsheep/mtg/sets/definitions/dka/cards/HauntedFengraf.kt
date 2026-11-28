package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Haunted Fengraf
 * Land
 * {T}: Add {C}.
 * {3}, {T}, Sacrifice this land: Return a creature card at random from your graveyard to your hand.
 *
 * Gather the creature cards in your graveyard, let the engine pick one at random
 * (SelectionMode.Random, no player choice), and put it into your hand. Not targeted, so it
 * resolves (doing nothing) with no creature cards in the graveyard.
 */
val HauntedFengraf = card("Haunted Fengraf") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{3}, {T}, Sacrifice this land: Return a creature card at random from your graveyard to your hand."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}"),
            Costs.Tap,
            Costs.SacrificeSelf
        )
        effect = Effects.Pipeline {
            val creatures = gather(
                CardSource.FromZone(zone = Zone.GRAVEYARD, player = Player.You, filter = GameObjectFilter.Creature)
            )
            val chosen = chooseRandom(1, from = creatures)
            toHand(chosen)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "157"
        artist = "Adam Paquette"
        flavorText = "A ghoulcaller's playground."
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84bca9e1-c0b7-4dce-9ee4-370db2c322b6.jpg?1783940790"
    }
}
