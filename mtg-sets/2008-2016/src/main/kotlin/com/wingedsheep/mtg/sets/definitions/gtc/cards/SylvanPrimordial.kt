package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.OwnerDistribution

/**
 * Sylvan Primordial — Gatecrash #136
 * {5}{G}{G} · Creature — Avatar · 6/8
 *
 * Reach
 * When this creature enters, for each opponent, destroy target noncreature permanent that player
 * controls. Search your library for up to X Forest cards, where X is the number of permanents
 * destroyed this way, put those cards onto the battlefield tapped, then shuffle.
 *
 * "For each opponent, … target … that player controls" is one target declaration distributed over
 * the opponents by projected controller ([OwnerDistribution.EXACTLY_ONE] with
 * `distributeByController`): one target per opponent who controls a noncreature permanent, so a
 * permanent an opponent stole still counts for that opponent. X counts only what was actually
 * destroyed (an indestructible or regenerated target, or one gone by resolution, doesn't count).
 */
val SylvanPrimordial = card("Sylvan Primordial") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Avatar"
    power = 6
    toughness = 8
    oracleText = "Reach\nWhen this creature enters, for each opponent, destroy target noncreature permanent " +
        "that player controls. Search your library for up to X Forest cards, where X is the number of " +
        "permanents destroyed this way, put those cards onto the battlefield tapped, then shuffle."

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(
            TargetFilter(GameObjectFilter.NoncreaturePermanent.opponentControls()),
            ownerDistribution = OwnerDistribution.EXACTLY_ONE,
            distributeByController = true,
        )
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            val destroyed = moveTracked(chosen, CardDestination.ToZone(Zone.GRAVEYARD), moveType = MoveType.Destroy)
            run(
                Patterns.Library.searchLibrary(
                    filter = GameObjectFilter.Any.withSubtype(Subtype.FOREST),
                    count = destroyed.count,
                    destination = SearchDestination.BATTLEFIELD,
                    entersTapped = true,
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "136"
        artist = "Stephan Martiniere"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/0483c869-38dc-4b0b-82f3-dd08a1ab985f.jpg?1789988913"
    }
}
