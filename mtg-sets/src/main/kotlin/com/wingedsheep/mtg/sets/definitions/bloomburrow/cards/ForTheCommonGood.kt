package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * For the Common Good
 * {X}{X}{G}
 * Sorcery
 *
 * Create X tokens that are copies of target token you control. Then tokens you control
 * gain indestructible until your next turn. You gain 1 life for each token you control.
 */
val ForTheCommonGood = card("For the Common Good") {
    manaCost = "{X}{X}{G}"
    typeLine = "Sorcery"
    oracleText = "Create X tokens that are copies of target token you control. Then tokens you control gain indestructible until your next turn. You gain 1 life for each token you control."

    spell {
        val token = target("token you control", TargetObject(
            filter = TargetFilter(baseFilter = GameObjectFilter.Token.youControl())
        ))
        effect = CreateTokenCopyOfTargetEffect(
            target = token,
            count = DynamicAmount.XValue
        ).then(
            EffectPatterns.grantKeywordToAll(
                keyword = Keyword.INDESTRUCTIBLE,
                filter = GroupFilter(baseFilter = GameObjectFilter.Token.youControl()),
                duration = Duration.UntilYourNextTurn
            )
        ).then(
            Effects.GainLife(
                DynamicAmount.Count(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Token.youControl())
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "172"
        artist = "Serena Malyon"
        flavorText = "Despite their differences, every creature and culture of Valley stood in unison against the threat."
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3ec72a27-b622-47d7-bdf3-970ccaef0d2a.jpg?1721426807"
    }
}
