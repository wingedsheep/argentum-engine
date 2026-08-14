package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player

/**
 * The Notary Hobbits — The Hobbit #131
 * {3}{G}{G} · Legendary Creature — Halfling Advisor · Rare
 * 1/1
 *
 * When The Notary Hobbits enter, if they're not a token, create two tokens that are copies of them,
 * except the tokens aren't legendary.
 * {T}: Add {C} for each Halfling you control.
 *
 * Modeling notes:
 *  - The "if they're not a token" clause is an intervening-if (CR 603.4), so it is a
 *    `triggerCondition` rather than a guard inside the effect — the ability never goes on the stack
 *    at all for the copies. That gate is what stops the copies from recursing: each token copy
 *    carries the same ETB trigger, and without it two tokens would make four, and so on.
 *  - `removeLegendary = true` is what "except the tokens aren't legendary" means, and it is also what
 *    keeps the legend rule (CR 704.5j) from immediately eating them.
 *  - The mana ability counts Halflings you control including The Notary Hobbits themself and the two
 *    tokens, so the printed card taps for {C}{C}{C} on an otherwise empty board.
 */
val TheNotaryHobbits = card("The Notary Hobbits") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Halfling Advisor"
    power = 1
    toughness = 1
    oracleText = "When The Notary Hobbits enter, if they're not a token, create two tokens that " +
        "are copies of them, except the tokens aren't legendary.\n" +
        "{T}: Add {C} for each Halfling you control."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.SourceMatches(GameObjectFilter.Any.nontoken())
        effect = Effects.CreateTokenCopyOfSelf(count = 2, removeLegendary = true)
        description = "When The Notary Hobbits enter, if they're not a token, create two tokens " +
            "that are copies of them, except the tokens aren't legendary."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(
            DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Creature.withSubtype("Halfling")
            ).count()
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add {C} for each Halfling you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "131"
        artist = "Jarel Threat"
        flavorText = "Since Bilbo was presumed dead, his belongings were auctioned by three of the " +
            "town's best lawyers: Messrs. Grubb, Grubb, and Burrowes."
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d876315f-b269-4254-a517-905c6e927462.jpg?1785412540"
    }
}
