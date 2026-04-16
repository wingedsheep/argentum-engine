package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests H1 / H2 / H3 from [`backlog/modal-cast-time-choices-plan.md`]:
 * per-mode additional costs (rule 700.2h) are paid when those modes are chosen.
 *
 * - H1: `Mode.additionalManaCost` is added to the spell's effective cost.
 * - H2: `Mode.additionalCosts` (e.g., `SacrificePermanent`) are collected and
 *   paid — the sacrificed permanent ends up in the graveyard after casting.
 * - H3: When multiple modes each declare an `additionalManaCost` and the player
 *   picks several, the extra costs **stack** (700.2h). The engine unions the
 *   per-mode cost lists via [CastSpellHandler.resolveAdditionalCostsForMode].
 */
class ModalPerModeAdditionalCostTest : FunSpec({

    // Mode 0: free "Gain 1 life"
    // Mode 1: "Draw a card" with extra {1}
    // Mode 2: "You lose 1 life" with extra {2}
    // chooseCount = 3, allowRepeat = false — lets us pick each mode once.
    val StackingCostsModal = CardDefinition(
        name = "Test Stacking Costs Modal",
        manaCost = ManaCost.parse("{R}"),
        typeLine = TypeLine.sorcery(),
        oracleText = "Choose up to three —\n• Gain 1 life\n• {1}: Draw a card\n• {2}: You lose 1 life",
        script = CardScript.spell(
            effect = ModalEffect(
                modes = listOf(
                    Mode.noTarget(
                        GainLifeEffect(DynamicAmount.Fixed(1), EffectTarget.Controller),
                        "Gain 1 life"
                    ),
                    Mode(
                        effect = DrawCardsEffect(DynamicAmount.Fixed(1), EffectTarget.Controller),
                        description = "Pay {1}: Draw a card",
                        additionalManaCost = "{1}"
                    ),
                    Mode(
                        effect = LoseLifeEffect(DynamicAmount.Fixed(1), EffectTarget.Controller),
                        description = "Pay {2}: You lose 1 life",
                        additionalManaCost = "{2}"
                    )
                ),
                chooseCount = 3,
                minChooseCount = 1
            )
        )
    )

    // Mode 0: sacrifice-a-creature → gain 3 life
    // Mode 1: no cost → gain 1 life
    val SacrificeModal = CardDefinition(
        name = "Test Sacrifice Modal",
        manaCost = ManaCost.parse("{R}"),
        typeLine = TypeLine.sorcery(),
        oracleText = "Choose two —\n• As an additional cost, sacrifice a creature. Gain 3 life.\n• Gain 1 life.",
        script = CardScript.spell(
            effect = ModalEffect(
                modes = listOf(
                    Mode(
                        effect = GainLifeEffect(DynamicAmount.Fixed(3), EffectTarget.Controller),
                        description = "Sacrifice a creature; Gain 3 life",
                        additionalCosts = listOf(
                            AdditionalCost.SacrificePermanent(
                                filter = GameObjectFilter.Creature,
                                count = 1
                            )
                        )
                    ),
                    Mode.noTarget(
                        GainLifeEffect(DynamicAmount.Fixed(1), EffectTarget.Controller),
                        "Gain 1 life"
                    )
                ),
                chooseCount = 2
            )
        )
    )

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(StackingCostsModal, SacrificeModal))
        return d
    }

    fun nameOf(d: GameTestDriver, id: EntityId): String? =
        d.state.getEntity(id)?.get<CardComponent>()?.name

    test("H1 — picking the {1}-cost mode drains one extra colorless from the pool") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Base cost: {R}. Mode 1 costs an additional {1}. Give exactly {R} + {1}.
        d.giveMana(p1, Color.RED, 1)
        d.giveColorlessMana(p1, 1)

        val handSizeBefore = d.state.getHand(p1).size
        val spell = d.putCardInHand(p1, "Test Stacking Costs Modal")

        // Cast with mode 1 pre-selected. paymentStrategy=FromPool because we pre-loaded mana.
        d.submit(
            CastSpell(
                playerId = p1,
                cardId = spell,
                chosenModes = listOf(1),
                paymentStrategy = com.wingedsheep.engine.core.PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true

        d.bothPass()

        // Mode 1 resolved: drew 1 card. Net hand: handSizeBefore + 1 put - 1 cast + 1 draw.
        d.state.getHand(p1).size shouldBe (handSizeBefore + 1)
    }

    test("H1 — mode 1 cannot be cast without the additional {1}") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Only give {R} — exactly enough for base but not for the additional {1}.
        d.giveMana(p1, Color.RED, 1)

        val spell = d.putCardInHand(p1, "Test Stacking Costs Modal")
        val result = d.submit(
            CastSpell(
                playerId = p1,
                cardId = spell,
                chosenModes = listOf(1),
                paymentStrategy = com.wingedsheep.engine.core.PaymentStrategy.FromPool
            )
        )

        // Cast must not succeed — insufficient mana after adding mode 1's {1}.
        result.isSuccess shouldBe false
    }

    test("H2 — mode with SacrificePermanent additional cost forces the creature into the graveyard") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // The creature we'll sacrifice.
        val sacrifice = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        // Mana for base {R}.
        d.giveMana(p1, Color.RED, 1)

        val lifeBefore = d.state.getEntity(p1)!!.get<LifeTotalComponent>()!!.life
        val spell = d.putCardInHand(p1, "Test Sacrifice Modal")

        // Pick modes [0, 1]. Mode 0's additional cost requires a creature sacrifice.
        d.submit(
            CastSpell(
                playerId = p1,
                cardId = spell,
                chosenModes = listOf(0, 1),
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(sacrifice)),
                paymentStrategy = com.wingedsheep.engine.core.PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true

        // After validation but before resolution, the creature is sacrificed (701.16 —
        // additional costs are paid before the spell goes on the stack).
        d.findPermanent(p1, "Grizzly Bears") shouldBe null
        d.getGraveyardCardNames(p1).contains("Grizzly Bears") shouldBe true

        d.bothPass()

        // Modes 0 and 1: gain 3 life + gain 1 life = 4 total.
        val lifeAfter = d.state.getEntity(p1)!!.get<LifeTotalComponent>()!!.life
        lifeAfter shouldBe (lifeBefore + 4)
    }

    test("H3 — Spree-style: choosing two modes stacks their additionalManaCosts") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Base cost {R}, + mode 1 {1} + mode 2 {2} = {R}{3} total.
        d.giveMana(p1, Color.RED, 1)
        d.giveColorlessMana(p1, 3)

        val lifeBefore = d.state.getEntity(p1)!!.get<LifeTotalComponent>()!!.life
        val handSizeBefore = d.state.getHand(p1).size
        val spell = d.putCardInHand(p1, "Test Stacking Costs Modal")

        // Pick modes [1, 2] — both have extra mana costs that must stack.
        d.submit(
            CastSpell(
                playerId = p1,
                cardId = spell,
                chosenModes = listOf(1, 2),
                paymentStrategy = com.wingedsheep.engine.core.PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true

        d.bothPass()

        // Modes resolved: draw 1 card + lose 1 life.
        d.state.getEntity(p1)!!.get<LifeTotalComponent>()!!.life shouldBe (lifeBefore - 1)
        // Hand transitions: handSizeBefore + 1 put - 1 cast + 1 draw = handSizeBefore + 1.
        d.state.getHand(p1).size shouldBe (handSizeBefore + 1)
    }

    test("H3 — with stacked costs unpayable, cast fails") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Only {R}{2} — enough for mode 1 alone ({R}{1}) but not for modes 1+2 ({R}{3}).
        d.giveMana(p1, Color.RED, 1)
        d.giveColorlessMana(p1, 2)

        val spell = d.putCardInHand(p1, "Test Stacking Costs Modal")
        val result = d.submit(
            CastSpell(
                playerId = p1,
                cardId = spell,
                chosenModes = listOf(1, 2),
                paymentStrategy = com.wingedsheep.engine.core.PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe false
    }
})
