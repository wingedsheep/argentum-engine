package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetSpellOrPermanent

/**
 * Commit // Memory — Amonkhet #211
 *
 * Commit {3}{U} — Instant: Put target spell or nonland permanent into its owner's library second
 *   from the top.
 * Memory {4}{U}{U} — Sorcery: Aftermath (Cast this spell only from your graveyard. Then exile it.)
 *   Each player shuffles their hand and graveyard into their library, then draws seven cards.
 *
 * Commit is a plain zone move at library index 1; a spell moved off the stack this way simply never
 * resolves (it isn't countered, so "can't be countered" doesn't stop it). Memory is Timetwister's
 * body: everyone shuffles hand + graveyard in before anyone draws. Memory is on the stack while it
 * resolves, so it never shuffles itself in, and aftermath exiles it afterward.
 */
val CommitMemory = card("Commit // Memory") {
    layout = CardLayout.SPLIT
    colorIdentity = "U"

    face("Commit") {
        manaCost = "{3}{U}"
        typeLine = "Instant"
        oracleText = "Put target spell or nonland permanent into its owner's library second from the top."

        spell {
            val t = target(
                TargetSpellOrPermanent(
                    permanentFilter = GameObjectFilter.NonlandPermanent,
                    descriptionOverride = "target spell or nonland permanent",
                )
            )
            effect = Effects.PutIntoLibraryNthFromTop(t, 1)
        }
    }

    face("Memory") {
        manaCost = "{4}{U}{U}"
        typeLine = "Sorcery"
        oracleText = "Aftermath (Cast this spell only from your graveyard. Then exile it.)\n" +
            "Each player shuffles their hand and graveyard into their library, then draws seven cards."
        keywords(Keyword.AFTERMATH)

        spell {
            effect = Effects.ForEachPlayer(
                players = Player.Each,
                Effects.Pipeline {
                    val handAndGraveyard = gather(
                        CardSource.FromMultipleZones(
                            zones = listOf(Zone.HAND, Zone.GRAVEYARD),
                            player = Player.You,
                        )
                    )
                    move(
                        handAndGraveyard,
                        CardDestination.ToZone(
                            Zone.LIBRARY,
                            Player.You,
                            ZonePlacement.Shuffled,
                        )
                    )
                },
            ) then Effects.ForEachPlayer(Player.Each, Effects.DrawCards(7))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "211"
        artist = "Ryan Alexander Lee"
        imageUri = "https://cards.scryfall.io/normal/front/0/6/06c9e2e8-2b4c-4087-9141-6aa25a506626.jpg?1783936458"
        ruling("2017-04-18", "If a spell is put into its owner's library, it's removed from the stack and thus will not resolve. The spell isn't countered; it just no longer exists. This works against a spell that can't be countered.")
        ruling("2017-04-18", "If a copy of a spell or a token is put into its owner's library, it's moved there, then it will cease to exist as a state-based action.")
        ruling("2017-04-18", "If the target's owner's library contains no cards, the target spell or permanent will be put into that player's library as the only card.")
        ruling("2017-04-18", "A spell with aftermath cast from a graveyard will always be exiled afterward, whether it resolves, it's countered, or it leaves the stack in some other way.")
    }
}
