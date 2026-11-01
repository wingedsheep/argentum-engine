package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Prime Speaker Vannifar
 * {2}{G}{U}
 * Legendary Creature — Elf Ooze Wizard
 * 2/4
 * {T}, Sacrifice another creature: Search your library for a creature card with mana value equal
 * to 1 plus the sacrificed creature's mana value, put that card onto the battlefield, then
 * shuffle. Activate only as a sorcery.
 *
 * The sacrificed creature's mana value is read from the cost-payment snapshot
 * ([EffectTarget.SacrificedAsCost], as in Neoform) and fed to the exact-equality mana-value
 * predicate.
 */
val PrimeSpeakerVannifar = card("Prime Speaker Vannifar") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Elf Ooze Wizard"
    power = 2
    toughness = 4
    oracleText = "{T}, Sacrifice another creature: Search your library for a creature card with " +
        "mana value equal to 1 plus the sacrificed creature's mana value, put that card onto the " +
        "battlefield, then shuffle. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.SacrificeAnother(GameObjectFilter.Creature)
        )
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Creature.manaValueEqualsDynamic(
                1 + DynamicAmounts.manaValueOf(EffectTarget.SacrificedAsCost(0))
            ),
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            shuffleAfter = true
        )
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "195"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84abfc59-10a7-4cb5-9cdd-81797116c810.jpg?1783933641"
    }
}
