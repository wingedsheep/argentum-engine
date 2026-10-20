package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Reincarnation — {1}{G}{G} Instant (Legends #201).
 *
 * "Choose target creature. When that creature dies this turn, return a creature card from its
 * owner's graveyard to the battlefield under the control of that creature's owner."
 *
 * A watched-creature delayed trigger. The dead creature is the trigger's triggering entity, so
 * [Player.OwnerOfTriggeringEntity] names the graveyard. Reincarnation's controller chooses the
 * card when the trigger resolves (the dead creature itself is eligible), and it enters under its
 * owner's control — the same player, since it came from that graveyard.
 */
val Reincarnation = card("Reincarnation") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Choose target creature. When that creature dies this turn, return a creature card from its " +
        "owner's graveyard to the battlefield under the control of that creature's owner."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.CreateDelayedTrigger(
            effect = Effects.Pipeline {
                val graveyard = gather(
                    CardSource.FromZone(Zone.GRAVEYARD, Player.OwnerOfTriggeringEntity, GameObjectFilter.Creature)
                )
                val returning = chooseExactly(
                    1,
                    graveyard,
                    prompt = "Choose a creature card to return to the battlefield"
                )
                move(returning, CardDestination.ToZone(Zone.BATTLEFIELD), underOwnersControl = true)
            },
            trigger = Triggers.self.dies(),
            watchedTarget = creature,
            expiry = DelayedTriggerExpiry.EndOfTurn,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "201"
        artist = "Edward P. Beard, Jr."
        imageUri = "https://cards.scryfall.io/normal/front/9/6/969c104e-daf4-480d-99a2-dd93c498b48e.jpg?1783948045"
    }
}
