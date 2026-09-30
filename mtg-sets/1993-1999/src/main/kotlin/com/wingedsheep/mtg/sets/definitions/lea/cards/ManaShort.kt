package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

val ManaShort = card("Mana Short") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Tap all lands target player controls and that player loses all unspent mana."

    spell {
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            val lands = gather(CardSource.ControlledPermanents(Player.TargetPlayer, GameObjectFilter.Land))
            run(Effects.TapCollection(lands))
        } then Effects.LoseUnspentMana(player)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "65"
        artist = "Dameon Willich"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73e3e0b3-5284-464f-8c62-0f7801c966f5.jpg?1783948704"
        ruling("2004-10-04", "If you play Mana Short in response to a spell, it will have no effect on that spell since the mana has already been paid.")
        ruling("2004-10-04", "It even taps lands that do not produce mana.")
    }
}
