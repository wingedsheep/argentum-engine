package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Monumental Henge
 * Land
 * This land enters tapped unless you control a Plains.
 * {T}: Add {W}.
 * {2}{W}{W}, {T}: Look at the top five cards of your library. You may reveal a historic card from
 * among them and put it into your hand. Put the rest on the bottom of your library in a random
 * order. (Artifacts, legendaries, and Sagas are historic.)
 */
val MonumentalHenge = card("Monumental Henge") {
    typeLine = "Land"
    colorIdentity = "W"
    oracleText = "This land enters tapped unless you control a Plains.\n{T}: Add {W}.\n{2}{W}{W}, {T}: Look at the top five cards of your library. You may reveal a historic card from among them and put it into your hand. Put the rest on the bottom of your library in a random order. (Artifacts, legendaries, and Sagas are historic.)"

    replacementEffect(EntersTapped(
        unlessCondition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Plains"))
    ))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{W}{W}"), Costs.Tap)
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 5,
            filter = GameObjectFilter.Historic,
            prompt = "You may reveal a historic card from among them and put it into your hand",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "222"
        artist = "Steven Belledin"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62907e7b-e531-4f51-9a69-7e60ae525775.jpg?1783911239"
    }
}
