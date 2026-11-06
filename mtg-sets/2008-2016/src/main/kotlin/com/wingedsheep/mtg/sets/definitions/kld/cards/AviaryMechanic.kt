package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Aviary Mechanic — Kaladesh #6
 * {1}{W} · Creature — Dwarf Artificer · 2/2
 *
 * When this creature enters, you may return another permanent you control to its owner's hand.
 *
 * The bounce doesn't target (Temur Sabertooth shape): the permanent is chosen as the trigger
 * resolves, from the permanents you control other than this creature.
 */
val AviaryMechanic = card("Aviary Mechanic") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dwarf Artificer"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, you may return another permanent you control to its owner's hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        description = "When this creature enters, you may return another permanent you control to its owner's hand."
        effect = Effects.May(
            Effects.Pipeline {
                val candidates = gather(
                    CardSource.BattlefieldMatching(
                        filter = GameObjectFilter.Permanent,
                        player = Player.You,
                        excludeSelf = true
                    )
                )
                val bounced = chooseExactly(
                    1,
                    from = candidates,
                    prompt = "Return another permanent you control to its owner's hand",
                    useTargetingUI = true
                )
                toHand(bounced)
            }
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "6"
        artist = "Mark Zug"
        flavorText = "Pilots take crippled skyships to the Aviaries, trusting Ghirapur's most talented " +
            "mechanics to get them back into the air."
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b97b0767-4308-4e5d-bc12-6cf8d8724797.jpg?1783937237"
    }
}
