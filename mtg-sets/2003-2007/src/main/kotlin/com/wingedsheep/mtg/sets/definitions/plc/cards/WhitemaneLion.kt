package com.wingedsheep.mtg.sets.definitions.plc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Whitemane Lion — Planar Chaos #22
 * {1}{W} · Creature — Cat · 2/2
 *
 * Flash
 * When this creature enters, return a creature you control to its owner's hand.
 *
 * The bounce doesn't target (Shrieking Drake shape): the creature is chosen as the trigger resolves,
 * and the Lion itself is a legal choice — the only one if you control no other creature.
 */
val WhitemaneLion = card("Whitemane Lion") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat"
    power = 2
    toughness = 2
    oracleText = "Flash\nWhen this creature enters, return a creature you control to its owner's hand."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        description = "When this creature enters, return a creature you control to its owner's hand."
        effect = Effects.Pipeline {
            val candidates = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Creature,
                    player = Player.You,
                )
            )
            val bounced = chooseExactly(
                1,
                from = candidates,
                prompt = "Return a creature you control to its owner's hand",
                useTargetingUI = true
            )
            toHand(bounced)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "22"
        artist = "Zoltan Boros & Gabor Szikszai"
        flavorText = "Saltfield nomads call a sudden storm a \"whitemane.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb1214ff-e959-43ca-8415-79a98d398490.jpg?1783943166"
        ruling(
            "2022-12-08",
            "You may return Whitemane Lion itself to its owner's hand as its triggered ability resolves. " +
                "If you don't control any other creature, you must return it."
        )
        ruling(
            "2022-12-08",
            "Whitemane Lion's ability doesn't target any creature, therefore no player may take actions " +
                "between the time you choose the creature to return and the time you return it."
        )
    }
}
