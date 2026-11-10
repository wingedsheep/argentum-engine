package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.LookAudience
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Inverter of Truth
 * {2}{B}{B}
 * Creature — Eldrazi
 * 6/6
 * Devoid (This card has no color.)
 * Flying
 * When this creature enters, exile all cards from your library face down, then shuffle all cards
 * from your graveyard into your library.
 *
 * Modelling notes:
 * - The library is gathered with [LookAudience.None] — moving it is not a look, and no player may
 *   see the cards (ruling) — then exiled face down with [FaceDownMode.HIDDEN] (Necropotence's
 *   shape), which has no turn-up procedure and is not lookable in exile.
 * - The graveyard is gathered only after the exile, so "then" holds: the shuffled-in cards are
 *   never exiled.
 */
val InverterOfTruth = card("Inverter of Truth") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Eldrazi"
    power = 6
    toughness = 6
    oracleText = "Devoid (This card has no color.)\n" +
        "Flying\n" +
        "When this creature enters, exile all cards from your library face down, then shuffle all " +
        "cards from your graveyard into your library."

    keywords(Keyword.DEVOID, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val library = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.You),
                lookAudience = LookAudience.None
            )
            exile(library, faceDown = FaceDownMode.HIDDEN)
            val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You))
            move(graveyard, CardDestination.ToZone(Zone.LIBRARY, Player.You, ZonePlacement.Shuffled))
        }
        description = "When this creature enters, exile all cards from your library face down, then " +
            "shuffle all cards from your graveyard into your library."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "72"
        artist = "Chase Stone"
        flavorText = "In its shadow, everything changed."
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88c9d8c5-333b-4281-b923-b4a060927bbc.jpg?1783937915"
        ruling("2016-01-22", "No player can look at the cards once they're exiled.")
        ruling("2016-01-22", "If your opponent puts one of the face-down exiled cards into your graveyard (for example, to activate an ability of an Eldrazi Processor), choose one of the cards at random. The card is revealed only after the cost is fully paid. That is, your opponent can't learn what card was chosen and decide to back up (not cast the spell, activate the ability, or so on, as applicable).")
    }
}
