package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.OwnerDistribution

/**
 * Diluvian Primordial — Gatecrash #33
 * {5}{U}{U} · Creature — Avatar · 5/5
 *
 * Flying
 * When this creature enters, for each opponent, you may cast up to one target instant or sorcery
 * card from that player's graveyard without paying its mana cost. If a spell cast this way would be
 * put into a graveyard, exile it instead.
 *
 * Targeting is Sepulchral Primordial's shape: one target declaration distributed over opponents'
 * graveyards ([OwnerDistribution.UP_TO_ONE]). At resolution the free-cast loop offers the surviving
 * targets one at a time, in the caster's order, and the caster may stop at any point ("you may").
 * The distribution already caps the casts at one per opponent, so the loop's own cap is unbounded.
 */
val DiluvianPrimordial = card("Diluvian Primordial") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Avatar"
    power = 5
    toughness = 5
    oracleText = "Flying\nWhen this creature enters, for each opponent, you may cast up to one target " +
        "instant or sorcery card from that player's graveyard without paying its mana cost. If a spell " +
        "cast this way would be put into a graveyard, exile it instead."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(
            TargetFilter(GameObjectFilter.InstantOrSorcery.ownedByOpponent(), zone = Zone.GRAVEYARD),
            ownerDistribution = OwnerDistribution.UP_TO_ONE,
        )
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            run(
                Effects.CastUpToNFromCollectionWithoutPayingCost(
                    chosen,
                    maxCasts = Int.MAX_VALUE,
                    insteadOfGraveyard = AfterResolveDestination.EXILE,
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "33"
        artist = "Stephan Martiniere"
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b7c34af-91de-44c6-a3e2-f48dbb0ce9fd.jpg?1783940139"
    }
}
