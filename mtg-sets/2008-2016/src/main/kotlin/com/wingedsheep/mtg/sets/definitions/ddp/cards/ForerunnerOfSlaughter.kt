package com.wingedsheep.mtg.sets.definitions.ddp.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Forerunner of Slaughter
 * {B}{R}
 * Creature — Eldrazi Drone
 * 3/2
 * Devoid (This card has no color.)
 * {1}: Target colorless creature gains haste until end of turn.
 *
 * Canonical here: Duel Decks: Zendikar vs. Eldrazi (2015-08-28) printed it five weeks before
 * Battle for Zendikar, which carries a `Printing` row.
 *
 * "Colorless" is read from projected state, so a devoid creature (itself included) is a legal
 * target and a creature that has been given a colour is not.
 */
val ForerunnerOfSlaughter = card("Forerunner of Slaughter") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Eldrazi Drone"
    power = 3
    toughness = 2
    oracleText = "Devoid (This card has no color.)\n" +
        "{1}: Target colorless creature gains haste until end of turn."

    keywords(Keyword.DEVOID)

    activatedAbility {
        cost = Costs.Mana("{1}")
        val t = target(TargetFilter(GameObjectFilter.Creature.withCardPredicate(CardPredicate.IsColorless)))
        effect = Effects.GrantKeyword(Keyword.HASTE, target = t)
        description = "{1}: Target colorless creature gains haste until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "James Zapata"
        flavorText = "\"While we delay, they're on the move. They will not wait as we bicker. " +
            "We are losing this war by moments as our goal slips beyond our reach.\"\n—Gideon Jura"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/195b867e-41ca-48af-bbd2-5e2c97411752.jpg?1783938236"
    }
}
