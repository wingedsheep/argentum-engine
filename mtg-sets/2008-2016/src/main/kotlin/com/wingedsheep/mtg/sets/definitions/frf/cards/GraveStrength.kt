package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Grave Strength
 * {1}{B}
 * Sorcery
 * Choose target creature. Mill three cards, then put a +1/+1 counter on that creature for each
 * creature card in your graveyard.
 *
 * The count is read when the counter step runs, after the mill, so the three cards just milled
 * are included (the Moldgraf Millipede shape, aimed at a target instead of itself). The target may
 * be any creature, but the count is always the caster's own graveyard. If the target is illegal
 * on resolution the spell's only target is gone and nothing happens, mill included.
 */
val GraveStrength = card("Grave Strength") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose target creature. Mill three cards, then put a +1/+1 counter on that creature " +
        "for each creature card in your graveyard. (To mill three cards, put the top three cards of " +
        "your library into your graveyard.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Patterns.Library.mill(3) then
            Effects.AddDynamicCounters(
                counterType = CounterType.PLUS_ONE_PLUS_ONE,
                amount = DynamicAmounts.creatureCardsInYourGraveyard(),
                target = creature,
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "71"
        artist = "Jeff Simpson"
        flavorText = "So long as death follows life, the supply lines of the Sultai will never be cut."
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72bc4463-69f6-4039-987d-e5db2ef256d5.jpg?1783938696"
    }
}
