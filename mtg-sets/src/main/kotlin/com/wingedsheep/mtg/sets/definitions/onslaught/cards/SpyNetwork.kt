package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.LookAtAllFaceDownCreaturesEffect
import com.wingedsheep.sdk.scripting.LookAtTargetHandEffect
import com.wingedsheep.sdk.scripting.LookAtTopAndReorderEffect
import com.wingedsheep.sdk.targeting.TargetPlayer

/**
 * Spy Network
 * {U}
 * Instant
 * Look at target player's hand, the top card of that player's library, and any
 * face-down creatures they control. Look at the top four cards of your library,
 * then put them back in any order.
 */
val SpyNetwork = card("Spy Network") {
    manaCost = "{U}"
    typeLine = "Instant"
    oracleText = "Look at target player's hand, the top card of that player's library, and any face-down creatures they control. Look at the top four cards of your library, then put them back in any order."

    spell {
        target = TargetPlayer()
        effect = LookAtTargetHandEffect(EffectTarget.ContextTarget(0))
            .then(LookAtTopAndReorderEffect(1, EffectTarget.ContextTarget(0)))
            .then(LookAtAllFaceDownCreaturesEffect(EffectTarget.ContextTarget(0)))
            .then(LookAtTopAndReorderEffect(4, EffectTarget.Controller))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "115"
        artist = "Andrew Robinson"
        flavorText = "\"Information is the most dangerous weapon.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a4bed3f-845c-4822-b8af-8b511dce6fe2.jpg?1562927629"
    }
}
