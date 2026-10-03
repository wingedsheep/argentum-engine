package com.wingedsheep.mtg.sets.definitions.vis.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Shrieking Drake
 * {U}
 * Creature — Drake
 * 1/1
 * Flying
 * When this creature enters, return a creature you control to its owner's hand.
 *
 * The bounce doesn't target: the creature is chosen as the trigger resolves (Gather → Select →
 * Move), and the Drake itself is a legal choice — the only one if you control no other creature.
 */
val ShriekingDrake = card("Shrieking Drake") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Drake"
    oracleText = "Flying\nWhen this creature enters, return a creature you control to its owner's hand."
    power = 1
    toughness = 1

    keywords(Keyword.FLYING)

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
        collectorNumber = "43"
        artist = "Ian Miller"
        flavorText = "\"Kaervek believes the drakes' cries herald his victory; in truth, they mourn aloud for his impending demise.\"\n—Teferi"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63971a64-c5f3-4d1f-ae0d-489d7d5b18f0.jpg?1783946997"
        ruling("2024-06-07", "You may return Shrieking Drake itself to its owner's hand as its triggered ability resolves. If you don't control any other creature, you must return it.")
        ruling("2024-06-07", "Shrieking Drake's ability doesn't target any creature. Therefore, no player may take actions between the time you choose the creature to return and the time you return it.")
    }
}
