package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.Effects

/**
 * Bringer of the Last Gift
 * {6}{B}{B}
 * Creature — Vampire Demon
 * 6/6
 *
 * Flying
 * When this creature enters, if you cast it, each player sacrifices all other creatures
 * they control. Then each player returns all creature cards from their graveyard that
 * weren't put there this way to the battlefield.
 *
 * Implementation: the "weren't put there this way" clause is handled by snapshotting the
 * creature cards already in every player's graveyard BEFORE the sacrifice, then returning
 * only that snapshot. Because [GatherCardsEffect] records entity ids without moving the
 * cards, the creatures sacrificed during this resolution land in graveyards afterwards and
 * are therefore absent from the snapshot. The return step uses `underOwnersControl = true`
 * so each reanimated creature enters under its owner's control, and the sacrifice uses
 * [MoveType.Sacrifice] so each permanent goes to its owner's graveyard (CR 701.21a). The
 * first gather spans all graveyards via the multi-player [Player.Each] reference.
 */
val BringerOfTheLastGift = card("Bringer of the Last Gift") {
    manaCost = "{6}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Demon"
    power = 6
    toughness = 6
    oracleText = "Flying\n" +
        "When this creature enters, if you cast it, each player sacrifices all other creatures they control. Then each player returns all creature cards from their graveyard that weren't put there this way to the battlefield."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.WasCast
        effect = Effects.Composite(
            listOf(
                // Snapshot every creature card already in a graveyard, before the sacrifice.
                // These are the cards "not put there this way" that will be returned.
                GatherCardsEffect(
                    source = CardSource.FromZone(
                        zone = Zone.GRAVEYARD,
                        player = Player.Each,
                        filter = GameObjectFilter.Creature
                    ),
                    storeAs = "graveyardCreaturesBeforeSacrifice"
                ),
                // Each player sacrifices all OTHER creatures they control (excludes this creature).
                GatherCardsEffect(
                    source = CardSource.BattlefieldMatching(
                        filter = GameObjectFilter.Creature,
                        player = Player.Each,
                        excludeSelf = true
                    ),
                    storeAs = "creaturesToSacrifice"
                ),
                MoveCollectionEffect(
                    from = "creaturesToSacrifice",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD),
                    moveType = MoveType.Sacrifice
                ),
                // Return the pre-existing graveyard creatures to the battlefield under their
                // owners' control. The just-sacrificed creatures are not in this snapshot.
                MoveCollectionEffect(
                    from = "graveyardCreaturesBeforeSacrifice",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD),
                    underOwnersControl = true
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "94"
        artist = "Wero Gallo"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19775c18-4cc0-49d3-86e4-0841768cbf4d.jpg?1779253388"

        ruling("2023-11-10", "Bringer of the Last Gift's last ability triggers if you cast it from any zone. It doesn't trigger if you put Bringer of the Last Gift onto the battlefield without casting it.")
    }
}
