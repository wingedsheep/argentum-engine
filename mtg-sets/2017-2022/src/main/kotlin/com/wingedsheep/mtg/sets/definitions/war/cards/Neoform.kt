package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Neoform
 * {G}{U}
 * Sorcery
 * As an additional cost to cast this spell, sacrifice a creature.
 * Search your library for a creature card with mana value equal to 1 plus the sacrificed
 * creature's mana value, put that card onto the battlefield with an additional +1/+1 counter on
 * it, then shuffle.
 *
 * The sacrificed creature's mana value is read from the cost-payment snapshot
 * ([EffectTarget.SacrificedAsCost], as in Eldritch Evolution) and fed to the exact-equality
 * mana-value predicate. The found card enters with its extra counter via the move's
 * `addCounterType`, so it is on the creature as it enters (not added afterwards).
 */
val Neoform = card("Neoform") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, sacrifice a creature.\n" +
        "Search your library for a creature card with mana value equal to 1 plus the sacrificed " +
        "creature's mana value, put that card onto the battlefield with an additional +1/+1 counter " +
        "on it, then shuffle."

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Creature))

    spell {
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(
                    Zone.LIBRARY,
                    Player.You,
                    GameObjectFilter.Creature.manaValueEqualsDynamic(
                        1 + DynamicAmounts.manaValueOf(EffectTarget.SacrificedAsCost(0))
                    )
                ),
                search = true
            )
            val found = chooseUpTo(
                1,
                from = searchable,
                prompt = "Search your library for a creature card with mana value equal to 1 plus " +
                    "the sacrificed creature's mana value"
            )
            move(
                found,
                CardDestination.ToZone(Zone.BATTLEFIELD),
                addCounterType = CounterType.PLUS_ONE_PLUS_ONE
            )
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "206"
        artist = "Bram Sels"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92d8f67e-4f2f-4a1f-b190-7c3f39e477e4.jpg?1783933392"
        ruling("2019-05-03", "You must sacrifice exactly one creature to cast this spell; you can't cast it without sacrificing one, and you can't sacrifice additional creatures.")
        ruling("2019-05-03", "If a creature or a creature card in your library has {X} in its mana cost, X is considered to be 0.")
        ruling("2019-05-03", "A creature put onto the battlefield this way enters the battlefield with one +1/+1 counter if it would otherwise enter with no +1/+1 counters.")
    }
}
