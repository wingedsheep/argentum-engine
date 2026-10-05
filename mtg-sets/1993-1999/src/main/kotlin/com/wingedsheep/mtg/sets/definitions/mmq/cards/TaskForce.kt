package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Task Force
 * {2}{W}
 * Creature — Human Rebel
 * 1/3
 * Whenever this creature becomes the target of a spell or ability, it gets +0/+3 until end of turn.
 */
val TaskForce = card("Task Force") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Rebel"
    oracleText = "Whenever this creature becomes the target of a spell or ability, it gets +0/+3 until end of turn."
    power = 1
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.becomesTarget()
        effect = Effects.ModifyStats(0, 3, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "52"
        artist = "Gary Ruddell"
        flavorText = "They are the reflection of Rushwood's glow on the edge of a wooden sword."
        imageUri = "https://cards.scryfall.io/normal/front/1/7/17a58c5b-28c2-4261-992c-2ecadb721880.jpg?1783945972"

        ruling(
            "2004-10-04",
            "The +0/+3 bonus is added as a triggered ability upon the casting/activating of a spell or " +
                "ability which targets this card."
        )
    }
}
