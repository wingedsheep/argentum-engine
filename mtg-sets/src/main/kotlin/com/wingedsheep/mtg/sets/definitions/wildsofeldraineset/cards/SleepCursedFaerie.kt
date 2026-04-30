package com.wingedsheep.mtg.sets.definitions.wildsofeldraineset.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sleep-Cursed Faerie
 * {U}
 * Creature — Faerie Wizard
 * 3/3
 *
 * Flying, ward {2}
 * This creature enters tapped with three stun counters on it.
 * {1}{U}: Untap this creature.
 */
val SleepCursedFaerie = card("Sleep-Cursed Faerie") {
    manaCost = "{U}"
    typeLine = "Creature — Faerie Wizard"
    power = 3
    toughness = 3
    oracleText = "Flying, ward {2}\nThis creature enters tapped with three stun counters on it. (If it would become untapped, remove a stun counter from it instead.)\n{1}{U}: Untap this creature."

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.ward("{2}"))

    replacementEffect(EntersTapped())
    replacementEffect(EntersWithCounters(
        counterType = CounterTypeFilter.Named(Counters.STUN),
        count = 3,
        selfOnly = true
    ))

    activatedAbility {
        cost = AbilityCost.Mana(ManaCost.parse("{1}{U}"))
        effect = Effects.Untap(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "66"
        artist = "Heonhwa"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31051436-68f2-457e-8293-2b10ccf7684e.jpg?1692937247"
    }
}
