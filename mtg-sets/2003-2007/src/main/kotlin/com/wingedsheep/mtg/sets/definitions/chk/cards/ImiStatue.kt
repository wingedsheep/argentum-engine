package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.UntapLimitPerStep

/**
 * Imi Statue
 * {3}
 * Artifact
 * Players can't untap more than one artifact during their untap steps.
 *
 * The same global untap-count cap as Damping Field ([UntapLimitPerStep], artifacts, max 1). It
 * applies to every player's untap step, and Imi Statue is itself an artifact it counts against.
 */
val ImiStatue = card("Imi Statue") {
    manaCost = "{3}"
    typeLine = "Artifact"
    oracleText = "Players can't untap more than one artifact during their untap steps."

    staticAbility {
        ability = UntapLimitPerStep(GameObjectFilter.Artifact, 1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "255"
        artist = "Todd Lockwood"
        flavorText = "\"Just looking at it fills me with dread . . . . Yet since it arrived, I've done little else. My blades have dulled and my business has failed, and still I cannot look away.\"\n—Keisaku, master swordmaker"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91c33332-8a72-4b81-a8e9-a73aaac6013c.jpg?1783944279"
    }
}
