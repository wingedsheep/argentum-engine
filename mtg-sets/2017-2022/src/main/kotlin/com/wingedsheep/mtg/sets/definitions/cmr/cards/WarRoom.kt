package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * War Room — Commander Legends #361.
 *
 * The life cost is priced from the activating player's commanders as the cost is paid: the
 * number of colors in the union of their color identities (a Partner or Background pair counts
 * both). A colorless commander makes it cost 0 life. With no commander at all the amount can't be
 * determined, so the ability can't be activated (see the rulings) — the cost pricer reports a
 * commander-identity count for a commanderless payer as unpayable.
 */
val WarRoom = card("War Room") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{3}, {T}, Pay life equal to the number of colors in your commanders' color identity: Draw a card."
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
            Costs.PayLife(DynamicAmounts.commanderColorIdentityCount()),
        )
        effect = Effects.DrawCards(1)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "361"
        artist = "Milivoj Ćeran"
        flavorText = "A figure is moved on a map, and the tide of war changes."
        imageUri = "https://cards.scryfall.io/normal/front/4/8/48d6ce7c-5dc8-449b-acbd-db259ae687ed.jpg?1783928737"
        ruling("2020-11-10", "Color identity is set before the game begins and doesn't change during the game, even if your commander is in a hidden zone (like the hand or library) or an effect changes your commander's color.")
        ruling("2020-11-10", "If you don't have a commander, you can't activate War Room's last ability at all.")
        ruling("2020-11-10", "If your commander has no colors in its color identity, you pay no life to activate War Room's last ability.")
    }
}
