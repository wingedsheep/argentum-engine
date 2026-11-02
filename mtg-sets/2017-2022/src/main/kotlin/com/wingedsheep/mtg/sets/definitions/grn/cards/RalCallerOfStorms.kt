package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Ral, Caller of Storms — Guilds of Ravnica #265 (planeswalker deck; canonical printing)
 * {4}{U}{R} · Legendary Planeswalker — Ral · Loyalty 4
 *
 * +1: Draw a card.
 * −2: Ral deals 3 damage divided as you choose among one, two, or three targets.
 * −7: Draw seven cards. Ral deals 7 damage to each creature your opponents control.
 *
 * The −2 is Ral, Monsoon Mage's / Inferno Titan's divided damage: one to three [AnyTarget]s with the
 * division chosen as the ability goes on the stack; illegal targets' share is simply not dealt (ruling).
 * The −7 is Cosmotronic Wave's sweep over creatures your opponents control.
 */
val RalCallerOfStorms = card("Ral, Caller of Storms") {
    manaCost = "{4}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Planeswalker — Ral"
    startingLoyalty = 4
    oracleText = "+1: Draw a card.\n" +
        "−2: Ral deals 3 damage divided as you choose among one, two, or three targets.\n" +
        "−7: Draw seven cards. Ral deals 7 damage to each creature your opponents control."

    // +1: Draw a card.
    loyaltyAbility(+1) {
        effect = Effects.DrawCards(1)
    }

    // −2: 3 damage divided among one, two, or three targets.
    loyaltyAbility(-2) {
        target = AnyTarget(count = 3, minCount = 1)
        effect = Effects.DividedDamage(total = 3, minTargets = 1, maxTargets = 3)
    }

    // −7: Draw seven cards, then 7 damage to each creature your opponents control.
    loyaltyAbility(-7) {
        effect = Effects.DrawCards(7) then
            Patterns.Group.dealDamageToAll(7, GroupFilter.AllCreaturesOpponentsControl)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "265"
        inBooster = false
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c44f0c60-5732-4c25-9ba9-4829f1891762.jpg?1783934096"
        ruling(
            "2018-10-05",
            "If some of the targets become illegal for Ral's second ability, the original division of " +
                "damage still applies, and the damage that would have been dealt to illegal targets isn't " +
                "dealt at all."
        )
    }
}
