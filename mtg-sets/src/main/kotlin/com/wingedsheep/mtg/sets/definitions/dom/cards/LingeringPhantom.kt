package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects

/**
 * Lingering Phantom
 * {5}{B}
 * Creature — Spirit
 * 5/4
 * Whenever you cast a historic spell, you may pay {B}. If you do, return Lingering Phantom
 * from your graveyard to your hand. (Artifacts, legendaries, and Sagas are historic.)
 */
val LingeringPhantom = card("Lingering Phantom") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Spirit"
    power = 5
    toughness = 4
    oracleText = "Whenever you cast a historic spell, you may pay {B}. If you do, return Lingering Phantom from your graveyard to your hand. (Artifacts, legendaries, and Sagas are historic.)"

    triggeredAbility {
        trigger = Triggers.YouCastHistoric
        triggerZone = Zone.GRAVEYARD
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{B}"),
            effect = Effects.Move(EffectTarget.Self, Zone.HAND)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "99"
        artist = "YW Tang"
        flavorText = "\"True power never dies, but lies awake, waiting for its name to be spoken.\"\n—The Eldest Reborn"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/2316239e-8fe6-467f-87e6-a3f4d19b860e.jpg?1562732666"
    }
}
