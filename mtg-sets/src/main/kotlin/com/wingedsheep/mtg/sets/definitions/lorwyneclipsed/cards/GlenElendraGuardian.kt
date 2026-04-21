package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Glen Elendra Guardian
 * {2}{U}
 * Creature — Faerie Wizard
 * 3/4
 *
 * Flash
 * Flying
 * This creature enters with a -1/-1 counter on it.
 * {1}{U}, Remove a counter from this creature: Counter target noncreature
 * spell. Its controller draws a card.
 */
val GlenElendraGuardian = card("Glen Elendra Guardian") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Faerie Wizard"
    power = 3
    toughness = 4
    oracleText = "Flash\nFlying\nThis creature enters with a -1/-1 counter on it.\n" +
        "{1}{U}, Remove a counter from this creature: Counter target noncreature spell. Its controller draws a card."

    keywords(Keyword.FLASH, Keyword.FLYING)

    replacementEffect(EntersWithCounters(
        counterType = CounterTypeFilter.MinusOneMinusOne,
        count = 1,
        selfOnly = true
    ))

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{U}"),
            Costs.RemoveCounterFromSelf(Counters.MINUS_ONE_MINUS_ONE)
        )
        target = Targets.NoncreatureSpell
        effect = Effects.CounterSpell()
            .then(Effects.DrawCards(1, target = EffectTarget.TargetController))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "51"
        artist = "Yohann Schepacz"
        flavorText = "\"The home of the fae must be protected.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/8/388d2e4a-0aa5-4b82-a86c-4777ca60161c.jpg?1767951867"
    }
}
