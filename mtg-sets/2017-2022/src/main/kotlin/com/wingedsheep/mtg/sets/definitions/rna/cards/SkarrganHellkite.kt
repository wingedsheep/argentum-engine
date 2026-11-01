package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.riot
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Skarrgan Hellkite — Ravnica Allegiance #114
 * {3}{R}{R} · Creature — Dragon · 4 / 4
 *
 * Riot picks between haste and the +1/+1 counter that switches the divided-damage ability on; the
 * gate reads the source's counters live, so a counter from any other source also enables it.
 */
val SkarrganHellkite = card("Skarrgan Hellkite") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 4
    oracleText = "Riot (This creature enters with your choice of a +1/+1 counter or haste.)\n" +
        "Flying\n" +
        "{3}{R}: This creature deals 2 damage divided as you choose among one or two targets. " +
        "Activate only if this creature has a +1/+1 counter on it."

    riot()
    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{3}{R}")
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.SourceHasCounter(CounterType.PLUS_ONE_PLUS_ONE))
        )
        target = AnyTarget(count = 2, minCount = 1)
        effect = Effects.DividedDamage(
            total = 2,
            minTargets = 1,
            maxTargets = 2
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "114"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be941a2e-4eb0-45b3-9da0-834053907a65.jpg?1783933678"
    }
}
