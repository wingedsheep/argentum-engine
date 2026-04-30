package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameEvent.AttackEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thoughtweft Imbuer
 * {3}{W}
 * Creature — Kithkin Advisor
 * 0/5
 *
 * Whenever a creature you control attacks alone, it gets +X/+X until end of turn,
 * where X is the number of Kithkin you control.
 */
val ThoughtweftImbuer = card("Thoughtweft Imbuer") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Kithkin Advisor"
    power = 0
    toughness = 5
    oracleText = "Whenever a creature you control attacks alone, it gets +X/+X until end of turn, " +
        "where X is the number of Kithkin you control."

    triggeredAbility {
        trigger = TriggerSpec(
            AttackEvent(filter = GameObjectFilter.Creature.youControl(), alone = true),
            TriggerBinding.ANY
        )
        val kithkinCount = DynamicAmounts.battlefield(
            com.wingedsheep.sdk.scripting.references.Player.You,
            GameObjectFilter.Creature.withSubtype(Subtype.KITHKIN)
        ).count()
        effect = Effects.ModifyStats(kithkinCount, kithkinCount, EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "38"
        artist = "Ioannis Fiore"
        flavorText = "\"Wear this, and you shall carry the might of the clachan with you.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e585a6c-2d95-450e-98cb-8794d7d8ecca.jpg?1767871768"

        ruling("2025-11-17", "The value of X is calculated only once, as Thoughtweft Imbuer's ability resolves.")
    }
}
