package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Keymaster Rogue
 * {3}{U}
 * Creature — Human Rogue
 * 3/2
 * This creature can't be blocked.
 * When this creature enters, return a creature you control to its owner's hand.
 */
val KeymasterRogue = card("Keymaster Rogue") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Rogue"
    oracleText = "This creature can't be blocked.\nWhen this creature enters, return a creature you control to its owner's hand."
    power = 3
    toughness = 2

    flags(AbilityFlag.CANT_BE_BLOCKED)

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
        collectorNumber = "39"
        artist = "Winona Nelson"
        flavorText = "\"This one opens the grate outside Zelzo Base. This one is for Ismeri Library. This one is for an invisible door . . . No, I've said too much.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/7/970ee9a3-a862-46a7-9aa5-7b6fc4ffa1ab.jpg"
    }
}
