package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Skullknocker Ogre
 * {3}{R}
 * Creature — Ogre
 * 4/3
 * Whenever this creature deals damage to an opponent, that player discards a card at random.
 * If the player does, they draw a card.
 */
val SkullknockerOgre = card("Skullknocker Ogre") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ogre"
    oracleText = "Whenever this creature deals damage to an opponent, that player discards a card at random. " +
        "If the player does, they draw a card."
    power = 4
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.dealsDamage(Recipient.Opponent)
        val damagedPlayer = EffectTarget.PlayerRef(Player.TriggeringPlayer)
        effect = Effects.IfYouDo(
            action = Patterns.Hand.discardRandom(1, damagedPlayer),
            then = Effects.DrawCards(1, damagedPlayer)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "142"
        artist = "Svetlin Velinov"
        flavorText = "The ogre crashed through the massive door. The winged folios scattered like frightened birds."
        imageUri = "https://cards.scryfall.io/normal/front/d/f/dfa89e28-c0a9-4b76-b0d5-8dcaa75bfd59.jpg?1783932617"
        ruling("2019-10-04", "If that player has one card in hand, it's discarded at random (even though that's not very random). The player draws a card.")
    }
}
