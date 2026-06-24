package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.RepeatDynamicTimesEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Valgavoth's Onslaught
 * {X}{X}{G}
 * Sorcery
 * Manifest dread X times, then put X +1/+1 counters on each of those creatures.
 *
 * The `{X}{X}` cost means the player pays 2X generic mana, but the *chosen value* of X (read via
 * [DynamicAmount.XValue]) is what drives the effect — same convention as Devastating Onslaught /
 * Another Round. So with X=3 the spell costs {6}{G}, manifests dread 3 times, then puts 3 +1/+1
 * counters on each of the 3 manifested creatures.
 *
 * Resolution:
 *   - [RepeatDynamicTimesEffect] runs [Patterns.Library.manifestDread] X times with
 *     `markEntered = true`, so each manifested permanent is stamped as "entered via this spell".
 *   - [GatherCardsEffect] from [CardSource.EnteredViaThisResolution] then collects *all* of those
 *     manifested creatures — it reads live battlefield state, so it survives the manifest-dread
 *     pick pauses and the per-iteration contexts of RepeatDynamicTimes (where a pipeline-collection
 *     accumulator would be overwritten each iteration).
 *   - [Effects.AddCountersToCollection] puts X +1/+1 counters on each gathered creature —
 *     "each of those creatures".
 *
 * Edge cases: with X=0 nothing happens (RepeatDynamicTimes no-ops, the gather is empty, no
 * counters); a manifest dread with fewer than two cards in the library manifests what's there
 * (CR 701.62); a manifested creature that has left the battlefield before counters are placed is
 * not gathered (the source restricts to permanents still on the battlefield).
 */
val ValgavothsOnslaught = card("Valgavoth's Onslaught") {
    manaCost = "{X}{X}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Manifest dread X times, then put X +1/+1 counters on each of those creatures. " +
        "(To manifest dread, look at the top two cards of your library, then put one onto the " +
        "battlefield face down as a 2/2 creature and the other into your graveyard. Turn it face " +
        "up any time for its mana cost if it's a creature card.)"

    spell {
        effect = Effects.Composite(
            listOf(
                RepeatDynamicTimesEffect(
                    amount = DynamicAmount.XValue,
                    body = Patterns.Library.manifestDread(markEntered = true)
                ),
                GatherCardsEffect(
                    source = CardSource.EnteredViaThisResolution,
                    storeAs = "valgavothManifested"
                ),
                Effects.AddCountersToCollection(
                    collectionName = "valgavothManifested",
                    counterType = Counters.PLUS_ONE_PLUS_ONE,
                    amount = DynamicAmount.XValue
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "204"
        artist = "Lie Setiawan"
        flavorText = "Tyvar and the Wanderer were done running."
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d4ba274-3c6f-4e12-ba2c-a81c3da3f7e2.jpg?1726286630"
    }
}
