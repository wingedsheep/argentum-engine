package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Ragavan, Nimble Pilferer — Modern Horizons 2 #138
 * {R} · Legendary Creature — Monkey Pirate · 2/1
 *
 * Whenever Ragavan deals combat damage to a player, create a Treasure token and exile the top
 * card of that player's library. Until end of turn, you may cast that card.
 * Dash {1}{R}
 *
 * The exile step reuses the Kotis, the Fangkeeper shape (gather from [Player.TriggeringPlayer]'s
 * library, move to exile), but swaps the free-cast-during-resolution finish for a lingering
 * [Effects.GrantMayPlayFromExile] permission: per the official ruling you must still follow all
 * timing restrictions and pay all costs when casting the exiled card (a land exiled this way
 * can't be played), so this is a normal-cost "may play" grant, not a synthesized free cast.
 * The Treasure and the exile are independent effects in the composite, matching the ruling that
 * the Treasure is created even if the opponent's library is empty.
 */
val RagavanNimblePilferer = card("Ragavan, Nimble Pilferer") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Monkey Pirate"
    power = 2
    toughness = 1
    oracleText = "Whenever Ragavan deals combat damage to a player, create a Treasure token " +
        "and exile the top card of that player's library. Until end of turn, you may cast " +
        "that card.\n" +
        "Dash {1}{R} (You may cast this spell for its dash cost. If you do, it gains haste, " +
        "and it's returned from the battlefield to its owner's hand at the beginning of the " +
        "next end step.)"

    dash = "{1}{R}"

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        description = "Whenever Ragavan deals combat damage to a player, create a Treasure " +
            "token and exile the top card of that player's library. Until end of turn, you " +
            "may cast that card."

        effect = Effects.Composite(
            listOf(
                Effects.CreateTreasure(1),
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(
                        DynamicAmount.Fixed(1),
                        player = Player.TriggeringPlayer
                    ),
                    storeAs = "exiled"
                ),
                MoveCollectionEffect(
                    from = "exiled",
                    destination = CardDestination.ToZone(Zone.EXILE, player = Player.TriggeringPlayer)
                ),
                Effects.GrantMayPlayFromExile(from = "exiled", nonLandOnly = true)
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "138"
        artist = "Simon Dominic"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9738cda-adb1-47fb-9f4c-ecd930228c4d.jpg?1783926839"
        ruling("2021-06-18", "You'll create a Treasure token even if that player has no cards left in their library to exile.")
        ruling("2021-06-18", "You must still follow all timing restrictions and pay all costs when casting the exiled card. If you exile a land card, you can't play that card.")
        ruling("2021-06-18", "If you choose to pay the dash cost rather than the mana cost, you're still casting the spell. It goes on the stack and can be responded to and countered. You can cast a creature spell for its dash cost only when you otherwise could cast that creature spell. Most of the time, this means during your main phase when the stack is empty.")
        ruling("2021-06-18", "If you pay the dash cost to cast a creature spell, that card will be returned to its owner's hand only if it's still on the battlefield when its triggered ability resolves. If it dies or goes to another zone before then, it will stay where it is.")
        ruling("2021-06-18", "You don't have to attack with the creature with dash unless another ability says you do.")
        ruling("2021-06-18", "If a creature enters the battlefield as a copy of or becomes a copy of a creature whose dash cost was paid, the copy won't have haste and won't be returned to its owner's hand.")
    }
}
