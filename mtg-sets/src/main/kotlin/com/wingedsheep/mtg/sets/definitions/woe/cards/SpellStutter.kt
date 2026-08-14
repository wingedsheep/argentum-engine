package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Spell Stutter
 * {1}{U}
 * Instant
 *
 * Counter target spell unless its controller pays {2} plus an additional {1} for each Faerie you
 * control.
 *
 * A scaling Force Spike: [Effects.CounterUnlessDynamicPays] over a single generic amount, since
 * "{2} plus an additional {1} for each Faerie" is arithmetic on one number rather than two separate
 * costs — `Add(Fixed(2), Count(…))`, the same composition [HobbitsSting] uses for "X plus Y".
 *
 * Two references worth being precise about:
 * - **"you"** is Spell Stutter's controller, not the countered spell's. The amount is evaluated
 *   against the counter effect's own [com.wingedsheep.sdk.scripting.EffectContext], so [Player.You]
 *   resolves to the Stutter caster while the *payment* is demanded of the targeted spell's
 *   controller. Faeries the victim controls don't make their own spell more expensive.
 * - **"each Faerie"**, not "each Faerie creature" — any permanent you control with the Faerie
 *   subtype counts, so the filter is [GameObjectFilter.Any]`.withSubtype("Faerie")` rather than a
 *   creature-restricted one.
 *
 * The count is taken as Spell Stutter resolves, so a Faerie that leaves before then doesn't add to
 * the tax; and because it's a real cost, an opponent who can't produce that much mana has the spell
 * countered without being offered the choice.
 */
val SpellStutter = card("Spell Stutter") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell unless its controller pays {2} plus an additional {1} for " +
        "each Faerie you control."

    spell {
        target = Targets.Spell
        effect = Effects.CounterUnlessDynamicPays(
            DynamicAmount.Add(
                DynamicAmount.Fixed(2),
                DynamicAmount.Count(
                    Player.You,
                    Zone.BATTLEFIELD,
                    GameObjectFilter.Any.withSubtype("Faerie"),
                ),
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "69"
        artist = "Liiga Smilshkalne"
        flavorText = "\"You are but learning to walk in a world where I fly, mortal.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24447e36-a42f-40a9-ad44-e904b6f9b276.jpg?1783915115"
    }
}
