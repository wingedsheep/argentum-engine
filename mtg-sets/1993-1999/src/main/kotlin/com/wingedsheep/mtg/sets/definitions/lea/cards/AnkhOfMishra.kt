package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ankh of Mishra — Limited Edition Alpha #230
 * {2} · Artifact
 *
 * Whenever a land enters, this artifact deals 2 damage to that land's controller.
 *
 * Zo-Zu the Punisher's shape: any land entering under any player's control (played or put onto
 * the battlefield by an effect) fires it, and the damage goes to the entering land's controller
 * via [EffectTarget.ControllerOfTriggeringEntity] — the Ankh's own controller included. No target.
 */
val AnkhOfMishra = card("Ankh of Mishra") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Whenever a land enters, this artifact deals 2 damage to that land's controller."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land).enters()
        effect = Effects.DealDamage(2, EffectTarget.ControllerOfTriggeringEntity)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "230"
        artist = "Amy Weber"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f594b7aa-d44e-47c4-989b-565f881e25f1.jpg?1783948670"
        ruling(
            "2004-10-04",
            "This triggers on any land entering. This includes playing a land or putting a land onto " +
                "the battlefield using a spell or ability."
        )
        ruling(
            "2004-10-04",
            "It determines the land's controller at the time the ability resolves. If the land leaves " +
                "the battlefield before the ability resolves, the land's last controller before it left is used."
        )
    }
}
