package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Battle Angels of Tyr — Commander Legends: Battle for Baldur's Gate #9
 * {2}{W}{W} · Creature — Angel Knight · 4/4
 *
 * Flying, myriad
 * Whenever this creature deals combat damage to a player, draw a card if that player has more
 * cards in hand than each other player. Then you create a Treasure token if that player controls
 * more lands than each other player. Then you gain 3 life if that player has more life than each
 * other player.
 *
 * Three independent [Conditions.PlayerHasMoreThanEachOtherPlayer] gates on the damaged player
 * ([Player.TriggeringPlayer]), each measured once per player with `Player.You` rebound to that
 * player. Every check is made as the ability resolves, and each is made even if an earlier one
 * failed (Scryfall rulings). "Each other player" includes a teammate in team games.
 */
val BattleAngelsOfTyr = card("Battle Angels of Tyr") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel Knight"
    power = 4
    toughness = 4
    oracleText = "Flying, myriad\n" +
        "Whenever this creature deals combat damage to a player, draw a card if that player has more cards " +
        "in hand than each other player. Then you create a Treasure token if that player controls more " +
        "lands than each other player. Then you gain 3 life if that player has more life than each other player."

    keywords(Keyword.FLYING)
    keywordAbilities(KeywordAbility.Simple(Keyword.MYRIAD))

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        val damaged = Player.TriggeringPlayer
        effect = Effects.If(
            Conditions.PlayerHasMoreThanEachOtherPlayer(damaged, DynamicAmounts.cardsInYourHand()),
            Effects.DrawCards(1)
        ) then Effects.If(
            Conditions.PlayerHasMoreThanEachOtherPlayer(damaged, DynamicAmounts.landsYouControl()),
            Effects.CreateTreasure()
        ) then Effects.If(
            Conditions.PlayerHasMoreThanEachOtherPlayer(damaged, DynamicAmounts.yourLifeTotal()),
            Effects.GainLife(3)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "9"
        artist = "Fajareka Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4c91dbf4-94fa-45d0-b8ed-a778b11d789b.jpg?1783922818"
    }
}
