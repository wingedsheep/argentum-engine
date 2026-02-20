package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.LookAtAllFaceDownCreaturesEffect
import com.wingedsheep.sdk.scripting.effects.LookAtTargetHandEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

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
            .then(
                CompositeEffect(
                    listOf(
                        GatherCardsEffect(
                            source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1), Player.ContextPlayer(0)),
                            storeAs = "target_top"
                        ),
                        MoveCollectionEffect(
                            from = "target_top",
                            destination = CardDestination.ToZone(
                                Zone.LIBRARY,
                                Player.ContextPlayer(0),
                                ZonePlacement.Top
                            ),
                            order = CardOrder.ControllerChooses
                        )
                    )
                )
            )
            .then(LookAtAllFaceDownCreaturesEffect(EffectTarget.ContextTarget(0)))
            .then(EffectPatterns.lookAtTopAndReorder(4))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "115"
        artist = "Andrew Robinson"
        flavorText = "\"Information is the most dangerous weapon.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a4bed3f-845c-4822-b8af-8b511dce6fe2.jpg?1562927629"
    }
}
