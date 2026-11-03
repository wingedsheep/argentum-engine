package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Infinite Diversity
 * {7}{R}{R}
 * Sorcery
 *
 * Federation — Exile target instant or sorcery card from your graveyard. Copy that card X times,
 * where X is the number of creature types among non-Borg creatures you control. You may cast the
 * copies without paying their mana costs. Exile Infinite Diversity.
 *
 * Mizzix's Mastery's exile → copy → free-cast chain, run X times: X is counted once as the spell
 * resolves (`DynamicAmounts.federation()`, evaluated up front by [Effects.Repeat]), and each pass
 * copies the card exiled this way and offers that copy for free. The copies are identical and all
 * cast during this resolution, so offering them one at a time is the printed "you may cast the
 * copies" — each is its own "may", timing restrictions are ignored, and an uncast copy ceases to
 * exist (CR 707.10a). `moveTracked` keeps "that card" literal: if the target doesn't reach exile,
 * nothing is copied. With X = 0 the card is still exiled and nothing is cast.
 */
val InfiniteDiversity = card("Infinite Diversity") {
    manaCost = "{7}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Federation — Exile target instant or sorcery card from your graveyard. Copy that card X " +
        "times, where X is the number of creature types among non-Borg creatures you control. You may " +
        "cast the copies without paying their mana costs. Exile Infinite Diversity."

    spell {
        target(TargetFilter(GameObjectFilter.InstantOrSorcery.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            val exiled = moveTracked(chosen, CardDestination.ToZone(Zone.EXILE))
            run(
                Effects.Repeat(
                    DynamicAmounts.federation(),
                    Effects.Pipeline {
                        val copy = copyCards(exiled, name = "infiniteDiversityCopy")
                        run(
                            Effects.May(
                                Effects.CastFromCollectionWithoutPayingCost(copy),
                                descriptionOverride = "You may cast a copy without paying its mana cost.",
                            )
                        )
                    },
                )
            )
        }

        selfExile()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "153"
        artist = "Julie Dillon"
        flavorText = "Our differences make us stronger."
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33ade614-3593-4055-8ecc-2d9e4858ba19.jpg?1793273819"
    }
}
