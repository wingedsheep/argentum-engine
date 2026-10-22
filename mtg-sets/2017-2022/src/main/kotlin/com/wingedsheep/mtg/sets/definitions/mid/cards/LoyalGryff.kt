package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Loyal Gryff (Innistrad: Midnight Hunt #26)
 * {2}{W}
 * Creature — Hippogriff
 * 2/2
 * Flash
 * Flying
 * When this creature enters, you may return another creature you control to its owner's hand.
 *
 * The bounce doesn't target (per the 2021-09-24 ruling, the choice is made on resolution), so it is
 * a gather → `chooseExactly(1)` → `toHand` pipeline behind [Effects.May]. With no other creature,
 * nothing is chosen and nothing moves.
 */
val LoyalGryff = card("Loyal Gryff") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Hippogriff"
    oracleText = "Flash\nFlying\nWhen this creature enters, you may return another creature you control " +
        "to its owner's hand."
    power = 2
    toughness = 2

    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Effects.Pipeline {
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
        )
        description = "When this creature enters, you may return another creature you control to its " +
            "owner's hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "26"
        artist = "Ilse Gort"
        flavorText = "Silverstreak had lost her rider, but not her instinct to help humankind."
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b4d41ac4-3273-4722-aaa6-ceeba66cf591.jpg?1783925657"
        ruling(
            "2021-09-24",
            "You choose whether to return a creature to your hand and which creature to return as " +
                "Loyal Gryff's triggered ability resolves."
        )
    }
}
