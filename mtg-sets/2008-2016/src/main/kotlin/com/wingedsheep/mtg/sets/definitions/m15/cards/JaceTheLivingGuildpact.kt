package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Jace, the Living Guildpact — Magic 2015 #62
 * {2}{U}{U} · Legendary Planeswalker — Jace · Starting loyalty 5
 *
 * +1: Look at the top two cards of your library. Put one of them into your graveyard.
 * −3: Return another target nonland permanent to its owner's hand.
 * −8: Each player shuffles their hand and graveyard into their library. You draw seven cards.
 *
 * Modeling notes:
 *
 *  - **The +1** is the Celestus Sanctifier / Soulcipher Board idiom: [Patterns.Library.lookAtTopAndKeep]
 *    with the chosen card going to the graveyard and the other staying on top of the library (ruling).
 *  - **The −3** is [TargetFilter.OtherNonlandPermanent] (any controller's, never Jace himself) bounced
 *    with [Effects.ReturnToHand] — Aether Channeler's mode.
 *  - **The −8** is Timetwister's shuffle body under [Effects.ForEachPlayer], then only Jace's controller
 *    draws seven. Activated from exactly 8 loyalty, Jace is already in the graveyard when the ability
 *    resolves, so he is shuffled into his owner's library with the rest.
 */
val JaceTheLivingGuildpact = card("Jace, the Living Guildpact") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Planeswalker — Jace"
    startingLoyalty = 5
    oracleText = "+1: Look at the top two cards of your library. Put one of them into your graveyard.\n" +
        "−3: Return another target nonland permanent to its owner's hand.\n" +
        "−8: Each player shuffles their hand and graveyard into their library. You draw seven cards."

    // +1: Look at the top two cards of your library. Put one of them into your graveyard.
    loyaltyAbility(+1) {
        effect = Patterns.Library.lookAtTopAndKeep(
            count = 2,
            keepCount = 1,
            keepDestination = CardDestination.ToZone(Zone.GRAVEYARD),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top),
        )
        description = "Look at the top two cards of your library. Put one of them into your graveyard."
    }

    // −3: Return another target nonland permanent to its owner's hand.
    loyaltyAbility(-3) {
        val permanent = target(TargetFilter.OtherNonlandPermanent)
        effect = Effects.ReturnToHand(permanent)
        description = "Return another target nonland permanent to its owner's hand."
    }

    // −8: Each player shuffles their hand and graveyard into their library. You draw seven cards.
    loyaltyAbility(-8) {
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
        ) then Effects.DrawCards(7)
        description = "Each player shuffles their hand and graveyard into their library. You draw seven cards."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "62"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/99713bb4-186f-42b6-aa66-e94ec8858e6a.jpg?1783939191"
        ruling(
            "2014-07-18",
            "When the first ability resolves, the card you don't put into your graveyard stays on top of your library.",
        )
    }
}
