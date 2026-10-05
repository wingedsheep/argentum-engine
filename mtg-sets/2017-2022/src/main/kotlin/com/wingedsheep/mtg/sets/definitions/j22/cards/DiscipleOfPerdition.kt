package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Disciple of Perdition — Jumpstart 2022 #23.
 * {1}{B} · Creature — Human Warlock · 1/3
 *
 * When this creature dies, choose one. If you have exactly 13 life, you may choose both instead.
 * • You draw a card and you lose 1 life.
 * • Exile target opponent's graveyard. That player loses 1 life.
 *
 * The conditional mode count is the Depth Defiler / Molten Collapse shape on a trigger: the cap
 * is 2 when you have exactly 13 life and 1 otherwise, while the floor is pinned at 1 — "choose
 * one" stays mandatory, "you may choose both" is optional. The trigger processor evaluates both
 * once, as the ability goes onto the stack, which is when modes are chosen (CR 603.3c).
 */
val DiscipleOfPerdition = card("Disciple of Perdition") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warlock"
    power = 1
    toughness = 3
    oracleText = "When this creature dies, choose one. If you have exactly 13 life, you may choose both instead.\n" +
        "• You draw a card and you lose 1 life.\n" +
        "• Exile target opponent's graveyard. That player loses 1 life."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.Modal(
            modes = listOf(
                mode("You draw a card and you lose 1 life") {
                    effect = Effects.DrawCards(1) then
                        Effects.LoseLife(1, EffectTarget.PlayerRef(Player.You))
                },
                mode("Exile target opponent's graveyard. That player loses 1 life") {
                    val opponent = target(Targets.Opponent)
                    effect = Effects.Pipeline {
                        val perditionGraveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, opponent.asPlayer))
                        exile(perditionGraveyard, opponent.asPlayer)
                    } then Effects.LoseLife(1, opponent)
                },
            ),
            dynamicChooseCount = DynamicAmounts.conditional(
                Conditions.CompareAmounts(DynamicAmounts.lifeTotal(Player.You), ComparisonOperator.EQ, 13),
                2,
                1,
            ),
            dynamicMinChooseCount = DynamicAmounts.fixed(1),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "23"
        artist = "Alix Branwyn"
        flavorText = "\"Could you please spare some of your blood for my precious little tree?\""
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e9abb9b-5d52-401f-aa6b-189aad72bfc2.jpg?1783919187"
    }
}
