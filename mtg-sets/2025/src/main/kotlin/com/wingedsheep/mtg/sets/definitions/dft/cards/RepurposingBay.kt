package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Repurposing Bay
 * {2}{U}
 * Artifact
 * {2}, {T}, Sacrifice another artifact: Search your library for an artifact card with mana value
 * equal to 1 plus the sacrificed artifact's mana value, put that card onto the battlefield, then
 * shuffle. Activate only as a sorcery.
 *
 * Cost-linked mana value, so this is the Sidisi, Regent of the Mire chain rather than
 * [com.wingedsheep.sdk.dsl.Patterns.Library.searchLibrary] — that facade takes a static
 * [GameObjectFilter], and there is no "mana value equals <computed>" card predicate. Target
 * validation also runs before cost payment, so the sacrificed artifact's mana value simply isn't
 * knowable at activation time; it has to be read at resolution.
 *
 * The chain mirrors what `searchLibrary(... destination = BATTLEFIELD)` expands to, with the
 * dynamic filter spliced in after the gather:
 *   1. gather every artifact card in your library,
 *   2. keep those whose mana value equals the sacrificed artifact's MV + 1 — read from the cost's
 *      sacrificed permanent via `EffectTarget.SacrificedAsCost(0)` (CR 112.7a / 608.2h; an {X} in the
 *      sacrificed artifact's cost counts as 0, per the 2025-02-07 ruling),
 *   3. choose up to one — `ChooseUpTo`, because searching never requires finding (CR 701.23b),
 *   4. put it onto the battlefield, shuffle, then emit the search event so
 *      "whenever a player searches their library" triggers see it.
 */
val RepurposingBay = card("Repurposing Bay") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "{2}, {T}, Sacrifice another artifact: Search your library for an artifact card " +
        "with mana value equal to 1 plus the sacrificed artifact's mana value, put that card onto " +
        "the battlefield, then shuffle. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.Tap,
            Costs.SacrificeAnother(GameObjectFilter.Artifact)
        )
        timing = TimingRule.SorcerySpeed
        effect = Effects.Pipeline {
            val libraryArtifacts = gather(CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Artifact), search = true)
            val candidates = filter(
                libraryArtifacts,
                GameObjectFilter.Any.manaValueEqualsDynamic(
                    DynamicAmounts.manaValueOf(EffectTarget.SacrificedAsCost(0)) + 1
                )
            )
            val found = chooseUpTo(1, from = candidates)
            move(found, CardDestination.ToZone(Zone.BATTLEFIELD))
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
        }
        description = "{2}, {T}, Sacrifice another artifact: Search your library for an artifact " +
            "card with mana value equal to 1 plus the sacrificed artifact's mana value, put that " +
            "card onto the battlefield, then shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "56"
        artist = "William Tempest"
        flavorText = "By the end of the race, few of the Aether Rangers' vehicles resembled what they started as."
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0cf1ace1-b7f5-4bd9-a494-ee7cb6c1f854.jpg?1783907906"
        ruling(
            "2025-02-07",
            "If there's an {X} in the sacrificed artifact's mana cost, X is 0 when determining its mana value."
        )
    }
}
