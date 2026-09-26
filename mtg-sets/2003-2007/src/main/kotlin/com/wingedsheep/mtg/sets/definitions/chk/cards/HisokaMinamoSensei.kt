package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hisoka, Minamo Sensei — Champions of Kamigawa #66 (canonical printing)
 * {2}{U}{U} · Legendary Creature — Human Wizard · 1/3
 *
 * {2}{U}, Discard a card: Counter target spell if it has the same mana value as the discarded card.
 *
 * The discard is a cost, so the card is gone before the ability resolves; the activation records
 * it and `EffectTarget.DiscardedAsCost` reads its mana value from the graveyard, compared against
 * the target spell's mana value at resolution.
 */
val HisokaMinamoSensei = card("Hisoka, Minamo Sensei") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Wizard"
    power = 1
    toughness = 3
    oracleText = "{2}{U}, Discard a card: Counter target spell if it has the same mana value as the discarded card."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{U}"), Costs.Discard())
        target(TargetFilter.SpellOnStack)
        effect = Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.targetManaValue(),
                ComparisonOperator.EQ,
                DynamicAmounts.manaValueOf(EffectTarget.DiscardedAsCost()),
            ),
            then = Effects.CounterSpell(),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "66"
        artist = "Donato Giancola"
        flavorText = "\"By all rights we should have perished in the Kami War. Our perseverence is a tribute to " +
            "mortal ingenuity. And perhaps a few forgotten secrets found in the nick of time.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a87aea05-5970-455e-a728-668cf23940a6.jpg?1783944327"
    }
}
