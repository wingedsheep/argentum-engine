package com.wingedsheep.mtg.sets.definitions.ala.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tezzeret the Seeker
 * {3}{U}{U}
 * Legendary Planeswalker — Tezzeret
 * Loyalty 4
 * +1: Untap up to two target artifacts.
 * −X: Search your library for an artifact card with mana value X or less, put it onto the
 *     battlefield, then shuffle.
 * −5: Artifacts you control become artifact creatures with base power and toughness 5/5 until
 *     end of turn.
 *
 * −X reads the X locked in when the loyalty cost is paid (`manaValueAtMostX()` resolves against
 * the ability's chosen X). −5 fixes the affected set at resolution (CR 611.2c) and each artifact
 * keeps its other types — the same shape as The Antiquities War's chapter III.
 */
val TezzeretTheSeeker = card("Tezzeret the Seeker") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Planeswalker — Tezzeret"
    startingLoyalty = 4
    oracleText = "+1: Untap up to two target artifacts.\n" +
        "−X: Search your library for an artifact card with mana value X or less, put it onto the battlefield, then shuffle.\n" +
        "−5: Artifacts you control become artifact creatures with base power and toughness 5/5 until end of turn."

    loyaltyAbility(+1) {
        targets(TargetFilter.Artifact, count = 2, optional = true)
        effect = Effects.ForEachTarget(Effects.Untap(EffectTarget.ContextTarget(0)))
    }

    loyaltyAbilityX {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Artifact.manaValueAtMostX(),
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
        )
    }

    loyaltyAbility(-5) {
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Artifact.youControl()),
            effect = Effects.BecomeCreature(
                target = EffectTarget.IterationEntity,
                power = 5,
                toughness = 5,
                duration = Duration.EndOfTurn
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "60"
        artist = "Anthony Francisco"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b214b6f-4734-4200-8467-92d7e3469b5d.jpg?1783942571"
        ruling("2008-10-01", "The first ability can target zero, one, or two artifacts. You may activate it with no targets just to put a loyalty counter on Tezzeret.")
        ruling("2008-10-01", "For the second ability, you choose the value of X when you activate it. You don't look through your library until the ability resolves. You can't choose an X that's greater than the number of loyalty counters on Tezzeret.")
        ruling("2008-10-01", "The third ability affects all artifacts you control, including artifacts that are already creatures.")
        ruling("2008-10-01", "The third ability causes artifacts you control to become creatures in addition to their other card types.")
    }
}
