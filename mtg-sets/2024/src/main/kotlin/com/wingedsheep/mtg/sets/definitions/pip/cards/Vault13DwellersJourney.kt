package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vault 13: Dweller's Journey (PIP #26)
 * {3}{W} · Enchantment — Saga
 *
 * I — For each player, exile up to one other target enchantment or creature that player controls
 *     until this Saga leaves the battlefield.
 * II — You gain 2 life and scry 2.
 * III — Return two cards exiled with this Saga to the battlefield under their owners' control and
 *       put the rest on the bottom of their owners' libraries.
 *
 * - Chapter I is the per-player distribution of Sheoldred's chapter I widened to every player
 *   (you included): up to one target per player, no two sharing a controller, the Saga itself
 *   excluded ("other"). Each target is exiled with [Effects.ExileUntilLeaves], which links it to the
 *   Saga and does nothing if the Saga already left (2024-03-08 ruling). The leaves trigger returns
 *   whatever is still linked — so after chapter III, when the Saga is sacrificed, it finds nothing.
 * - Chapter III: you choose two of the linked cards (all of them when two or fewer) and return them
 *   under their owners' control. The rest go to the bottom of their owners' libraries, each owner
 *   ordering their own cards (2024-03-08 ruling) — one pass per owner still in the linked pile.
 */
val Vault13DwellersJourney = card("Vault 13: Dweller's Journey") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — For each player, exile up to one other target enchantment or creature that player " +
        "controls until this Saga leaves the battlefield.\n" +
        "II — You gain 2 life and scry 2.\n" +
        "III — Return two cards exiled with this Saga to the battlefield under their owners' control " +
        "and put the rest on the bottom of their owners' libraries."

    sagaChapter(1) {
        targets(
            TargetFilter.CreatureOrEnchantment.other(),
            optional = true,
            dynamicMaxCount = DynamicAmounts.playerCount(Player.Each),
            differentControllers = true,
        )
        effect = Effects.ForEachTarget(Effects.ExileUntilLeaves(EffectTarget.ContextTarget(0)))
    }

    sagaChapter(2) {
        effect = Effects.GainLife(2) then Effects.Scry(2)
    }

    sagaChapter(3) {
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.FromLinkedExile())
            val (returning, _) = chooseExactlySplit(
                2,
                from = exiled,
                prompt = "Choose two cards to return to the battlefield",
                selectedLabel = "Return to the battlefield",
                remainderLabel = "Put on the bottom of its owner's library",
            )
            move(returning, CardDestination.ToZone(Zone.BATTLEFIELD), underOwnersControl = true)
        } then Effects.ForEachPlayer(
            players = Player.OwnersOfLinkedExile,
            Effects.Pipeline {
                val linked = gather(CardSource.FromLinkedExile())
                val owned = filter(linked, GameObjectFilter.Any.ownedByYou())
                toLibraryBottom(owned, Player.You, CardOrder.OwnerChooses)
            }
        )
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "26"
        artist = "Gaboleps"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a22ad436-215a-4da1-b12e-1faac474f948.jpg?1783912414"
        ruling(
            "2024-03-08",
            "If Vault 13 leaves the battlefield before its first chapter ability resolves, the target " +
                "permanents won't be exiled at all."
        )
        ruling(
            "2024-03-08",
            "In the unusual case where one player owns multiple cards exiled with Vault 13 that are being " +
                "put on the bottom of their library with its third chapter ability, the owner of those cards " +
                "chooses the order in which they are put on the bottom of their library."
        )
    }
}
