package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Tezzeret's Gatebreaker
 * {4}
 * Artifact
 * When this artifact enters, look at the top five cards of your library. You may reveal a blue or
 * artifact card from among them and put it into your hand. Put the rest on the bottom of your
 * library in a random order.
 * {5}{U}, {T}, Sacrifice this artifact: Creatures you control can't be blocked this turn.
 *
 * The enters trigger is Ashe, Princess of Dalmasca's [Patterns.Library.lookAtTopRevealMatchingToHand]
 * over a "blue or artifact" card filter. The activated ability is Jace, Arcane Strategist's −7: a
 * floating group restriction, so creatures that come under your control later in the turn are
 * covered too (the ruling below).
 */
val TezzeretsGatebreaker = card("Tezzeret's Gatebreaker") {
    manaCost = "{4}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, look at the top five cards of your library. You may reveal a " +
        "blue or artifact card from among them and put it into your hand. Put the rest on the bottom of " +
        "your library in a random order.\n" +
        "{5}{U}, {T}, Sacrifice this artifact: Creatures you control can't be blocked this turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 5,
            filter = GameObjectFilter.Any.withColor(Color.BLUE) or GameObjectFilter.Artifact,
            prompt = "You may reveal a blue or artifact card from among them and put it into your hand"
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}{U}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.CantBeBlockedGroup(GroupFilter.AllCreaturesYouControl)
        description = "{5}{U}, {T}, Sacrifice this artifact: Creatures you control can't be blocked this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "289"
        artist = "Mathias Kollros"
        inBooster = false
        imageUri = "https://cards.scryfall.io/normal/front/5/8/584e0528-01c2-49fb-aa4a-098172bb17a7.jpg?1783934487"

        ruling(
            "2018-07-13",
            "Because the last ability of Tezzeret's Gatebreaker doesn't change the characteristics of any " +
                "permanents, the set of creatures affected by it is constantly updated. Creatures that come " +
                "under your control later in the turn can't be blocked."
        )
    }
}
