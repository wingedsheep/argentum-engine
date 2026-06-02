package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Nobody
 * {1}{U/R}{U/R}
 * Artifact Creature — Human Hero
 * 3/2
 *
 * When this creature enters, return up to one other target artifact
 * you control to its owner's hand. Scry 1.
 */
val Nobody = card("Nobody") {
    manaCost = "{1}{U/R}{U/R}"
    colorIdentity = "UR"
    typeLine = "Artifact Creature — Human Hero"
    oracleText = "When this creature enters, return up to one other target artifact you control to its owner's hand. Scry 1. (Look at the top card of your library. You may put that card on the bottom.)"
    power = 3
    toughness = 2

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val artifact = target(
            "other artifact you control",
            TargetPermanent(optional = true, filter = TargetFilter.Artifact.youControl().other())
        )
        effect = Effects.ReturnToHand(artifact)
            .then(EffectPatterns.scry(1))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "161"
        artist = "Yuhong Ding"
        flavorText = "\"I don't need a name. I'm nobody.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/9/0966b8ff-61d3-4394-a357-97c35f042a29.jpg?1771587036"
    }
}
