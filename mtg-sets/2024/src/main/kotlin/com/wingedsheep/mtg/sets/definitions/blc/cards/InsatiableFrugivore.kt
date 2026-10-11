package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.RepeatCondition
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Insatiable Frugivore — Bloomburrow Commander #18
 * {3}{B} · Creature — Rat Berserker · 2/4
 *
 * The enters trigger is a do-while loop: each pass creates a Food, then offers to exile three
 * cards from your graveyard; the loop repeats only while a pass actually exiled them. Per the
 * 2024-07-26 ruling you can't try to exile three with fewer than three cards in your graveyard,
 * so the "may" is only offered at three or more — and with three or more, `chooseExactly(3)`
 * forces all three, so "exiled anything" is exactly "exiled three".
 *
 * "Sacrifice X Foods" is a variable-count sacrifice of any Food artifact (not only tokens, per
 * the rulings) whose count is the ability's X; X may be 0, granting just menace. The group is
 * named once and both the pump and menace are applied in one pass.
 */
val InsatiableFrugivore = card("Insatiable Frugivore") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Rat Berserker"
    power = 2
    toughness = 4
    oracleText = "When this creature enters, create a Food token, then you may exile three cards from your " +
        "graveyard. If you do, repeat this process.\n" +
        "{3}{B}, Sacrifice X Foods: Creatures you control get +X/+0 and gain menace until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.RepeatWhile(
            body = Effects.CreateFood() then
                Effects.If(
                    Conditions.CardsInGraveyardAtLeast(3),
                    Effects.May(
                        Effects.Pipeline {
                            val graveyardCards = gather(CardSource.FromZone(zone = Zone.GRAVEYARD))
                            // Named: the repeat condition reads it from outside the pipeline.
                            val toExile = chooseExactly(3, from = graveyardCards, selectedLabel = "Exile", name = "frugivoreExiled")
                            exile(toExile)
                        },
                        prompt = "Exile three cards from your graveyard to repeat this process?"
                    )
                ),
            repeatCondition = RepeatCondition.WhileCondition(
                Conditions.CollectionContainsMatch("frugivoreExiled")
            )
        )
        description = "When this creature enters, create a Food token, then you may exile three cards from " +
            "your graveyard. If you do, repeat this process."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}{B}"),
            Costs.SacrificePermanents(GameObjectFilter.Artifact.withSubtype("Food"), minCount = 0)
        )
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.ModifyStats(DynamicAmounts.xValue(), DynamicAmounts.fixed(0), EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.MENACE, EffectTarget.IterationEntity)
        )
        description = "{3}{B}, Sacrifice X Foods: Creatures you control get +X/+0 and gain menace until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "18"
        artist = "Ben Wootten"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2ef58a8f-ce72-40c2-b032-818cc16267ce.jpg?1783910731"
        ruling(
            "2024-07-26",
            "The process in Insatiable Frugivore's first ability is repeated until you decline to exile three " +
                "cards from your graveyard. You can't try to exile three cards from your graveyard if you don't " +
                "have at least three cards in your graveyard."
        )
        ruling(
            "2024-07-26",
            "You can choose 0 for the value of X in the cost of Insatiable Frugivore's last ability. If you do, " +
                "creatures you control will just gain menace until end of turn."
        )
    }
}
