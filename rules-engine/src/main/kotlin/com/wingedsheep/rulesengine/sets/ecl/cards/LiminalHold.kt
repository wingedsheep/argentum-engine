package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.CompositeEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.ExileUntilLeavesEffect
import com.wingedsheep.rulesengine.ability.GainLifeEffect
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

/**
 * Liminal Hold
 *
 * {3}{W} Enchantment
 * When this enchantment enters, exile up to one target nonland permanent
 * an opponent controls until this enchantment leaves the battlefield.
 * You gain 2 life.
 */
object LiminalHold {
    val definition = CardDefinition.enchantment(
        name = "Liminal Hold",
        manaCost = ManaCost.parse("{3}{W}"),
        oracleText = "When this enchantment enters, exile up to one target nonland permanent " +
                "an opponent controls until this enchantment leaves the battlefield. You gain 2 life.",
        metadata = ScryfallMetadata(
            collectorNumber = "24",
            rarity = Rarity.COMMON,
            artist = "Ovidio Cartagena",
            flavorText = "A reliquary is needed to pass through the eclipse safely. Without it, two warring selves will calcify into one.",
            imageUri = "https://cards.scryfall.io/normal/front/a/5/a5a40c16-7a5c-4ad1-be53-6b1b1be2affe.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Liminal Hold") {
        // ETB: Exile up to one target nonland permanent opponent controls until this leaves
        // Also gain 2 life
        triggered(
            trigger = OnEnterBattlefield(),
            effect = CompositeEffect(
                effects = listOf(
                    ExileUntilLeavesEffect(
                        target = EffectTarget.TargetOpponentNonlandPermanent
                    ),
                    GainLifeEffect(
                        amount = 2,
                        target = EffectTarget.Controller
                    )
                )
            ),
            optional = true  // "up to one" means the exile part is optional
        )
    }
}
