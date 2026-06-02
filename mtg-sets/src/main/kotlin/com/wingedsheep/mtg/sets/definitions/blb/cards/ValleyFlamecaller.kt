package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyDamageAmount
import com.wingedsheep.sdk.scripting.events.SourceFilter

/**
 * Valley Flamecaller
 * {2}{R}
 * Creature — Lizard Warlock
 * 3/3
 * If a Lizard, Mouse, Otter, or Raccoon you control would deal damage to a permanent or player,
 * it deals that much damage plus 1 instead.
 */
val ValleyFlamecaller = card("Valley Flamecaller") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Lizard Warlock"
    power = 3
    toughness = 3
    oracleText = "If a Lizard, Mouse, Otter, or Raccoon you control would deal damage to a permanent or player, it deals that much damage plus 1 instead."

    replacementEffect(
        ModifyDamageAmount(
            modifier = 1,
            appliesTo = EventPattern.DamageEvent(
                source = SourceFilter.Matching(
                    GameObjectFilter.Creature
                        .youControl()
                        .withAnyOfSubtypes(
                            listOf(
                                Subtype("Lizard"),
                                Subtype("Mouse"),
                                Subtype("Otter"),
                                Subtype("Raccoon")
                            )
                        )
                )
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "158"
        artist = "Justin Gerard"
        flavorText = "\"Well, that's another night of research spoiled.\"\n—Claire, batfolk astrologer"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a0812db4-b7d1-4cf7-aaa5-9c0e784079a1.jpg?1721639385"
    }
}
