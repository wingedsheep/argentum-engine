package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Chilling Grasp (Eldritch Moon #50)
 * {2}{U}
 * Instant
 *
 * Tap up to two target creatures. Those creatures don't untap during their controller's next
 * untap step.
 * Madness {3}{U}
 *
 * The Frost Breath spell shape: one "up to two" requirement, each target tapped and given
 * DOESNT_UNTAP keyed to its *own* controller's next untap step. Madness (CR 702.35) lets it be cast
 * for {3}{U} when discarded.
 */
val ChillingGrasp = card("Chilling Grasp") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Tap up to two target creatures. Those creatures don't untap during their controller's next untap step.\n" +
        "Madness {3}{U} (If you discard this card, discard it into exile. When you do, cast it " +
        "for its madness cost or put it into your graveyard.)"

    spell {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.Tap(EffectTarget.ContextTarget(0)),
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                EffectTarget.ContextTarget(0),
                Duration.UntilAfterAffectedControllersNextUntap()
            )
        )
    }

    madness("{3}{U}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "50"
        artist = "Seb McKinnon"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/60d85d58-eae3-4f9d-8f54-440fa6ded95b.jpg?1783937505"
    }
}
