package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Candlelit Cavalry (Innistrad: Midnight Hunt #175)
 * {4}{G}
 * Creature — Human Knight
 * 5/5
 * Coven — At the beginning of combat on your turn, if you control three or more creatures with
 * different powers, this creature gains trample until end of turn.
 */
val CandlelitCavalry = card("Candlelit Cavalry") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Knight"
    oracleText = "Coven — At the beginning of combat on your turn, if you control three or more creatures " +
        "with different powers, this creature gains trample until end of turn."
    power = 5
    toughness = 5

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        interveningIf = Conditions.CompareAmounts(
            left = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Creature,
            ).distinctValues(CardNumericProperty.POWER),
            operator = ComparisonOperator.GTE,
            right = 3,
        )
        effect = Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "175"
        artist = "Viko Menezes"
        flavorText = "\"On this night, the dark will fear us.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4c3bd60-1ff6-4136-9188-d0620fd299a7.jpg?1783925584"
    }
}
