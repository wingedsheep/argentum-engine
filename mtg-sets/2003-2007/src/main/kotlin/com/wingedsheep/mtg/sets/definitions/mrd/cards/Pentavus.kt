package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pentavus — Mirrodin #226
 * {7} · Artifact Creature — Construct · 0/0 · Rare
 *
 * This creature enters with five +1/+1 counters on it.
 * {1}, Remove a +1/+1 counter from this creature: Create a 1/1 colorless Pentavite artifact
 * creature token with flying.
 * {1}, Sacrifice a Pentavite: Put a +1/+1 counter on this creature.
 *
 * Modelling notes:
 * - Printed P/T is 0/0; the five [EntersWithCounters] +1/+1 counters (CR 613.4c, layer 7d) make
 *   it a 5/5 on the battlefield. Removing the last counter kills it as a state-based action —
 *   which is exactly right, since the fifth activation leaves a 0/0 behind.
 * - Both rulings are consequences of modelling the costs as plain [Costs.RemoveCounterFromSelf]
 *   and [Costs.Sacrifice] rather than tracking which counters/tokens Pentavus itself made:
 *   *any* +1/+1 counter on it pays the first cost, and *any* Pentavite you control (an Adaptive
 *   Automaton naming Pentavite, say) pays the second. Neither is scoped to this card's own output.
 * - Pentavus is a Construct, so it can never sacrifice itself to its own second ability.
 * - No `underOwnersControl` subtleties: the tokens are created by the controller and stay theirs.
 */
val Pentavus = card("Pentavus") {
    manaCost = "{7}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 0
    toughness = 0
    oracleText = "This creature enters with five +1/+1 counters on it.\n" +
        "{1}, Remove a +1/+1 counter from this creature: Create a 1/1 colorless Pentavite " +
        "artifact creature token with flying.\n" +
        "{1}, Sacrifice a Pentavite: Put a +1/+1 counter on this creature."

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 5,
            selfOnly = true
        )
    )

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.RemoveCounterFromSelf(CounterType.PLUS_ONE_PLUS_ONE)
        )
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Pentavite"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true,
            // Mirrodin printed no token cards; this is the Magic Player Rewards 2004 Pentavite,
            // the contemporary printing of the same art.
            imageUri = "https://cards.scryfall.io/normal/front/4/f/4fbb0dc6-2f9e-4389-8a90-531e94009bfb.jpg?1783944459"
        )
        description = "{1}, Remove a +1/+1 counter from this creature: Create a 1/1 colorless " +
            "Pentavite artifact creature token with flying."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.Sacrifice(GameObjectFilter.Permanent.withSubtype("Pentavite"))
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "{1}, Sacrifice a Pentavite: Put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "226"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32a11f0a-7547-4fda-a8ed-caf76ce98f10.jpg?1783944509"
        ruling(
            "2011-09-22",
            "You can remove any +1/+1 counter on Pentavus to activate its first activated ability, " +
                "not just ones created by Pentavus's other abilities."
        )
        ruling(
            "2011-09-22",
            "You can sacrifice any Pentavite creature you control (such as an Adaptive Automaton " +
                "with Pentavite as its chosen type) to activate Pentavus's second activated ability, " +
                "not just ones created by Pentavus's other abilities."
        )
    }
}
