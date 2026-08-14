package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Armor Wars — Marvel Super Heroes #203
 * {2}{U}{R} · Enchantment — Saga
 *
 * I — You may draw a card for each artifact you control. If you do, each opponent draws a card.
 * II — Artifact spells you cast this turn cost {1} less to cast.
 * III — This Saga deals X damage to target opponent, where X is the greatest mana value among
 *       artifacts you control.
 *
 * Modeling notes:
 *  - Chapter I is a single [MayEffect] wrapping *both* halves rather than an `IfYouDoEffect`.
 *    The "If you do" here keys off the may-choice, and there is no draw-flavoured
 *    `SuccessCriterion` to gate on, so a declined chapter runs neither draw and an accepted one
 *    runs both — which is the card's behaviour in every case that matters. The one wrinkle worth
 *    naming: with zero artifacts you may still say yes, draw nothing, and each opponent draws.
 *    Whether "you did" when the draw was for zero cards is not something the printed text
 *    settles; gating on the choice is the reading that keeps the two halves atomic.
 *  - Chapter II is the Will, Scion of Peace shape ([Effects.ReduceSpellCostsThisTurn]): a
 *    turn-scoped, state-held reduction that already scopes to the controller's own spells, is
 *    not consumed by the first matching spell, survives the Saga being sacrificed after III, and
 *    reduces only generic mana (CR 601.2f).
 *  - Chapter III targets an opponent and reads the greatest mana value among *your* artifacts at
 *    resolution ([DynamicAmounts.battlefield] MAX over `MANA_VALUE`). With no artifacts the
 *    aggregate is 0 and the Saga deals no damage. The Saga is the damage source by default.
 */
val ArmorWars = card("Armor Wars") {
    manaCost = "{2}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — You may draw a card for each artifact you control. If you do, each opponent draws a card.\n" +
        "II — Artifact spells you cast this turn cost {1} less to cast.\n" +
        "III — This Saga deals X damage to target opponent, where X is the greatest mana value " +
        "among artifacts you control."

    // I — You may draw a card for each artifact you control. If you do, each opponent draws a card.
    sagaChapter(1) {
        effect = MayEffect(
            effect = Effects.Composite(
                Effects.DrawCards(
                    DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count()
                ),
                Effects.DrawCards(1, EffectTarget.PlayerRef(Player.EachOpponent)),
            ),
            descriptionOverride = "You may draw a card for each artifact you control. " +
                "If you do, each opponent draws a card.",
        )
    }

    // II — Artifact spells you cast this turn cost {1} less to cast.
    sagaChapter(2) {
        effect = Effects.ReduceSpellCostsThisTurn(
            spellFilter = GameObjectFilter.Artifact,
            amount = DynamicAmount.Fixed(1),
        )
    }

    // III — This Saga deals X damage to target opponent, where X is the greatest mana value
    //       among artifacts you control.
    sagaChapter(3) {
        val opponent = target("target opponent", Targets.Opponent)
        effect = Effects.DealDamage(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).maxManaValue(),
            opponent,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "203"
        artist = "Serena Malyon"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11a13397-1d31-4257-87c2-a757a751c601.jpg?1783902906"
    }
}
