package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Justice, Vance Astrovik — Marvel Super Heroes #61
 * {2}{U} · Legendary Creature — Mutant Hero · 2/2
 *
 * Flying
 * When Justice enters, return up to one target nonland, nontoken permanent to its owner's hand.
 * Whenever another nonland permanent you control is returned to its owner's hand, put a +1/+1
 * counter on Justice.
 *
 * "Up to one target" is `TargetPermanent(optional = true, …)` — the ability may be put on the
 * stack with no target and then does nothing. The `nontoken()` predicate on the filter is part of
 * the *targeting* restriction, not a resolution check, so a token is never a legal choice.
 *
 * "Is returned to its owner's hand" is a battlefield → hand zone change of a permanent you
 * control, i.e. [Triggers.leavesBattlefield] with `to = Zone.HAND`. The [TriggerBinding.OTHER]
 * binding supplies the "another" — Justice bouncing itself never triggers it. Note the second
 * ability, unlike the first, is not restricted to nontoken permanents (bouncing a token does
 * trigger it, even though the token ceases to exist).
 */
val JusticeVanceAstrovik = card("Justice, Vance Astrovik") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Mutant Hero"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "When Justice enters, return up to one target nonland, nontoken permanent to its owner's hand.\n" +
        "Whenever another nonland permanent you control is returned to its owner's hand, put a +1/+1 counter on Justice."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target(
            "up to one target nonland, nontoken permanent",
            TargetPermanent(
                optional = true,
                filter = TargetFilter(GameObjectFilter.NonlandPermanent.nontoken()),
            )
        )
        effect = Effects.Move(t, Zone.HAND)
        description = "When Justice enters, return up to one target nonland, nontoken permanent to its owner's hand."
    }

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.NonlandPermanent.youControl(),
            to = Zone.HAND,
            binding = TriggerBinding.OTHER,
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever another nonland permanent you control is returned to its owner's hand, put a +1/+1 counter on Justice."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "61"
        artist = "Berto Martinez"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1448731-d50e-4b9c-8eb2-3d85c21516c6.jpg?1783902956"
    }
}
