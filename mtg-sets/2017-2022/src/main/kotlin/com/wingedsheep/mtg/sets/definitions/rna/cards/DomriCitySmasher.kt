package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Domri, City Smasher — Ravnica Allegiance #269 (planeswalker deck)
 * {4}{R}{G} Legendary Planeswalker — Domri, loyalty 4
 *
 * +2: Creatures you control get +1/+1 and gain haste until end of turn.
 * −3: Domri deals 3 damage to any target.
 * −8: Put three +1/+1 counters on each creature you control. Those creatures gain trample until
 *     end of turn.
 *
 * The −8 visits each creature once, putting the counters on it and granting it trample, so
 * "those creatures" is exactly the group that received counters.
 */
val DomriCitySmasher = card("Domri, City Smasher") {
    manaCost = "{4}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Planeswalker — Domri"
    startingLoyalty = 4
    oracleText = "+2: Creatures you control get +1/+1 and gain haste until end of turn.\n" +
        "−3: Domri deals 3 damage to any target.\n" +
        "−8: Put three +1/+1 counters on each creature you control. Those creatures gain trample until end of turn."

    loyaltyAbility(+2) {
        effect = Patterns.Group.pumpAndGrantToAll(1, 1, Keyword.HASTE, Filters.Group.creaturesYouControl)
    }

    loyaltyAbility(-3) {
        val t = target(Targets.Any)
        effect = Effects.DealDamage(3, t)
    }

    loyaltyAbility(-8) {
        effect = Effects.ForEachInGroup(
            filter = Filters.Group.creaturesYouControl,
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 3, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "269"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8fe16cdb-90df-4670-a084-55c505791d85.jpg?1783933610"
        ruling("2019-01-25", "Domri's first and third abilities affect only creatures you control at the time they resolve. Creatures you begin to control later in the turn won't get +1/+1 or gain haste if his first ability has resolved, and won't gain trample or get three +1/+1 counters if his third ability has resolved.")
    }
}
