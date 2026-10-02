package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
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

/**
 * Jor Kadeen, First Goldwarden
 * {R}{W}
 * Legendary Creature — Human Rebel
 * 2/2
 *
 * Trample
 * Whenever Jor Kadeen attacks, it gets +X/+X until end of turn, where X is the number of
 * equipped creatures you control. Then if Jor Kadeen's power is 4 or greater, draw a card.
 *
 * X is locked in as the pump resolves (per ruling); the power check is a resolution-time
 * "then if", not an intervening-if, so it reads Jor Kadeen's power after the pump.
 */
val JorKadeenFirstGoldwarden = card("Jor Kadeen, First Goldwarden") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human Rebel"
    power = 2
    toughness = 2
    oracleText = "Trample\nWhenever Jor Kadeen attacks, it gets +X/+X until end of turn, where X is the number of equipped creatures you control. Then if Jor Kadeen's power is 4 or greater, draw a card."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val x = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature.equipped()).count()
        effect = Effects.ModifyStats(x, x, EffectTarget.Self) then Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.sourcePower(),
                ComparisonOperator.GTE,
                4
            ),
            then = Effects.DrawCards(1)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "203"
        artist = "Jeremy Wilson"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7e4d8f7-874d-4b2a-ad23-afab479784ea.jpg?1783918001"
        ruling("2023-02-04", "The value of X is determined only once, as the triggered ability resolves. If the number of equipped creatures you control changes after that time, it won't change the bonus granted.")
    }
}
