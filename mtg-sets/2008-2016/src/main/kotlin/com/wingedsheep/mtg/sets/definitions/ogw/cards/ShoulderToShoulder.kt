package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shoulder to Shoulder — Oath of the Gatewatch #34 (canonical printing)
 * {2}{W} · Sorcery
 *
 * Support 2. (Put a +1/+1 counter on each of up to two target creatures.)
 * Draw a card.
 *
 * Support on an instant or sorcery is a keyword action with no "other" clause: one requirement taking
 * zero to two creatures (any controller), one +1/+1 counter on each, then an unconditional draw — Travel
 * Preparations' counters followed by Repel the Darkness's draw.
 */
val ShoulderToShoulder = card("Shoulder to Shoulder") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Support 2. (Put a +1/+1 counter on each of up to two target creatures.)\nDraw a card."

    spell {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0))
        ) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "34"
        artist = "Chris Rallis"
        flavorText = "\"Sea Gate may have fallen, but we still stand. We still fight.\"\n—Gideon Jura"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49d11865-a1ea-4f2e-b7da-31630c6d0272.jpg?1783937924"
        ruling("2016-01-22", "You can't put more than one +1/+1 counter on any one target using the support action.")
        ruling("2016-01-22", "Support can target a creature you don't control.")
        ruling("2016-01-22", "If a spell with support has other abilities that target creatures, those abilities and the support ability can target the same creature.")
    }
}
