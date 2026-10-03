package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Rush of Inspiration {1}{U/R}{U/R} // Crackling Falls
 * Instant
 * Draw two cards. Then discard a card at random unless you pay {E}{E} (two energy counters).
 * //
 * Land
 * This land enters tapped.
 * {T}: Add {U} or {R}.
 *
 * The energy payment is a resolution-time [Effects.PayOrSuffer]: with fewer than two energy the
 * random discard simply happens.
 */
private val RushOfInspirationFront = card("Rush of Inspiration") {
    manaCost = "{1}{U/R}{U/R}"
    colorIdentity = "UR"
    typeLine = "Instant"
    oracleText = "Draw two cards. Then discard a card at random unless you pay {E}{E} (two energy counters)."

    spell {
        effect = Effects.DrawCards(2) then Effects.PayOrSuffer(
            cost = Costs.pay.PayPlayerCounters(CounterType.ENERGY, 2),
            suffer = Patterns.Hand.discardRandom(1),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "257"
        artist = "Jorge Jacinto"
        flavorText = "\"Insight is like a waterfall—near-limitless power, but only if properly harnessed.\"\n—Saheeli Rai"
        imageUri = "https://cards.scryfall.io/normal/front/7/0/70a25a3a-c12a-49d3-8a91-a108dfa9d3c5.jpg?1783911226"
    }
}

private val CracklingFallsBack = card("Crackling Falls") {
    typeLine = "Land"
    colorIdentity = "UR"
    oracleText = "This land enters tapped.\n{T}: Add {U} or {R}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "257"
        artist = "Jorge Jacinto"
        flavorText = "In the wilds beyond Ghirapur, raw magical energy permeates the landscape."
        imageUri = "https://cards.scryfall.io/normal/back/7/0/70a25a3a-c12a-49d3-8a91-a108dfa9d3c5.jpg?1783911226"
    }
}

val RushOfInspiration: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = RushOfInspirationFront,
    backFace = CracklingFallsBack,
)
