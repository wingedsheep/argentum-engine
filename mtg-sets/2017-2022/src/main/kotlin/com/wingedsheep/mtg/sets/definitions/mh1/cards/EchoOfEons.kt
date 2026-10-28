package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Echo of Eons — Modern Horizons #46
 * {4}{U}{U} · Sorcery
 *
 * Each player shuffles their hand and graveyard into their library, then draws seven cards.
 * Flashback {2}{U}
 *
 * Timetwister's body: every player shuffles hand + graveyard into their library before anyone
 * draws seven. Echo of Eons is on the stack while it resolves, so it never shuffles itself in
 * (a flashback cast is exiled afterward by [KeywordAbility.Flashback]'s rider).
 */
val EchoOfEons = card("Echo of Eons") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Each player shuffles their hand and graveyard into their library, then draws seven cards.\n" +
        "Flashback {2}{U} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    keywordAbility(KeywordAbility.flashback("{2}{U}"))

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

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "46"
        artist = "Terese Nielsen"
        flavorText = "The present is a matter of perspective."
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff590af2-2d6c-4f16-a9b8-1a6dab6e9ad5.jpg?1783933147"
        ruling(
            "2019-06-14",
            "Echo of Eons won't be put into your graveyard until after it's finished resolving, which means " +
                "it won't be shuffled into your library as part of its own effect.",
        )
        ruling(
            "2021-03-19",
            "A spell cast using flashback will always be exiled afterward, whether it resolves, is countered, " +
                "or leaves the stack in some other way.",
        )
    }
}
