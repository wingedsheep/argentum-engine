package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Lord of the Void
 * {4}{B}{B}{B}
 * Creature — Demon
 * 7/7
 * Flying
 * Whenever this creature deals combat damage to a player, exile the top seven cards of that
 * player's library, then put a creature card from among them onto the battlefield under your
 * control.
 *
 * "That player" is the damaged player ([Player.TriggeringPlayer]). The seven cards are exiled
 * face up, then a mandatory pick of exactly one creature card from the exiled collection
 * (2013-01-24 ruling: not optional; auto-selects none if no creature was exiled) moves to the
 * battlefield under the ability's controller — `ToZone(BATTLEFIELD)` defaults to [Player.You].
 */
val LordOfTheVoid = card("Lord of the Void") {
    manaCost = "{4}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    power = 7
    toughness = 7
    oracleText = "Flying\nWhenever this creature deals combat damage to a player, exile the top seven " +
        "cards of that player's library, then put a creature card from among them onto the battlefield " +
        "under your control."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(7, player = Player.TriggeringPlayer))
            exile(exiled, Player.TriggeringPlayer)
            val stolen = chooseExactly(
                1,
                from = exiled,
                filter = GameObjectFilter.Creature,
                showAllCards = true,
                prompt = "Put a creature card onto the battlefield under your control"
            )
            move(stolen, CardDestination.ToZone(Zone.BATTLEFIELD))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "71"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75b83fe5-fd00-4532-bc67-07836abfc99c.jpg?1783940130"
        ruling(
            "2013-01-24",
            "Lord of the Void's ability isn't optional. If there is a creature card among the seven cards " +
                "you exiled, you must put one onto the battlefield under your control."
        )
    }
}
