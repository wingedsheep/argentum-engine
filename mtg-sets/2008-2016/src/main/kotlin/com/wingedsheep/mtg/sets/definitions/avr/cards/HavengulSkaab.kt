package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Havengul Skaab
 * {5}{U}
 * Creature — Zombie Horror
 * 4/5
 *
 * Whenever this creature attacks, return another creature you control to its owner's hand.
 *
 * Ambush Krotiq's untargeted bounce on an attack trigger: the creature is chosen on resolution, and with
 * no other creature the ability does nothing (2012-05-01 ruling).
 */
val HavengulSkaab = card("Havengul Skaab") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Zombie Horror"
    power = 4
    toughness = 5
    oracleText = "Whenever this creature attacks, return another creature you control to its owner's hand."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        description = "Whenever this creature attacks, return another creature you control to its owner's hand."
        effect = Effects.Pipeline {
            val candidates = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Creature,
                    player = Player.You,
                    excludeSelf = true
                )
            )
            val bounced = chooseExactly(
                1,
                from = candidates,
                prompt = "Return another creature you control to its owner's hand",
                useTargetingUI = true
            )
            toHand(bounced)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "60"
        artist = "Vincent Proce"
        flavorText = "An abomination so violent that the other fiends tremble at the sight of it."
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4c7ff97f-fb06-4a61-98cd-50965a6522d4.jpg?1783940717"
        ruling("2012-05-01", "If you don't control any other creatures, the ability won't do anything.")
    }
}
