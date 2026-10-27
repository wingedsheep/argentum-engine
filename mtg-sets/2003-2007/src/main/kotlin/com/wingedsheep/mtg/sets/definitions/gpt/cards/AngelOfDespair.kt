package com.wingedsheep.mtg.sets.definitions.gpt.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Angel of Despair
 * {3}{W}{W}{B}{B}
 * Creature — Angel
 * 5/5
 * Flying
 * When this creature enters, destroy target permanent.
 *
 * The target is any permanent — including lands and permanents you control.
 */
val AngelOfDespair = card("Angel of Despair") {
    manaCost = "{3}{W}{W}{B}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Angel"
    power = 5
    toughness = 5
    oracleText = "Flying\nWhen this creature enters, destroy target permanent."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Permanent)
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "101"
        artist = "Todd Lockwood"
        flavorText = "\"I feel in them a sense of duty and commitment, yet I can feel nothing else. It is as if their duty is to an empty void.\"\n—Razia"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9ae71424-df9c-481c-9305-a0c30adcfda2.jpg?1783943486"
    }
}
