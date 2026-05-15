package com.wingedsheep.mtg.sets.definitions.mbs.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Spine of Ish Sah
 * {7}
 * Artifact
 * When this artifact enters, destroy target permanent.
 * When this artifact is put into a graveyard from the battlefield, return it to its owner's hand.
 */
val SpineOfIshSah = card("Spine of Ish Sah") {
    manaCost = "{7}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "When this artifact enters, destroy target permanent.\n" +
        "When this artifact is put into a graveyard from the battlefield, return it to its owner's hand."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val permanent = target("permanent", Targets.Permanent)
        effect = Effects.Destroy(permanent)
    }

    triggeredAbility {
        trigger = Triggers.PutIntoGraveyardFromBattlefield
        effect = Effects.ReturnToHand(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "136"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59313b00-ac75-484a-ad74-db9d8960c0f8.jpg?1562611517"
        ruling(
            "2011-06-01",
            "Spine of Ish Sah's last ability doesn't allow you to sacrifice it. You must find another way to get Spine of Ish Sah into the graveyard."
        )
    }
}
