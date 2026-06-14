package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.ReturnTheFavor
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Return the Favor — {R}{R} Instant, Spree
 *
 * + {1} — Copy target instant spell, sorcery spell, activated ability, or triggered ability.
 *         You may choose new targets for the copy.
 * + {1} — Change the target of target spell or ability with a single target.
 */
class ReturnTheFavorScenarioTest : FunSpec({

    // Inline test creature whose ETB triggered ability targets a player and deals 2 damage —
    // exercises the triggered-ability branch of CopyTargetSpellOrAbility (and its retargeting).
    val ZapBeast = card("Zap Beast") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Beast"
        power = 2
        toughness = 2

        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            target = Targets.Player
            effect = Effects.DealDamage(2, EffectTarget.ContextTarget(0))
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ReturnTheFavor)
        driver.registerCard(ZapBeast)
        return driver
    }

    test("mode + {1} (copy): copy a target instant spell and retarget the copy") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        // Opponent casts Lightning Bolt at me; I copy it with Return the Favor and aim the copy
        // at the opponent.
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.passPriority(me)
        driver.castSpellWithTargets(opponent, bolt, listOf(ChosenTarget.Player(me)))
        val boltOnStack = driver.getTopOfStack()!!
        driver.passPriority(opponent)

        val rtf = driver.putCardInHand(me, "Return the Favor")
        driver.giveMana(me, Color.RED, 2)
        driver.giveColorlessMana(me, 1) // {R}{R} base + {1} for the copy mode
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = rtf,
                targets = listOf(ChosenTarget.Spell(boltOnStack)),
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Spell(boltOnStack))),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true

        // Resolve Return the Favor → pause to choose new targets for the copy.
        var guard = 0
        while (driver.state.pendingDecision !is ChooseTargetsDecision && guard < 20) {
            driver.bothPass()
            guard++
        }
        (driver.state.pendingDecision is ChooseTargetsDecision) shouldBe true

        // Aim the copy at the opponent (the original still targets me).
        driver.submitTargetSelection(me, listOf(opponent)).isSuccess shouldBe true

        // Resolve the copy, then the original. Opponent takes 3 from the copy, I take 3 from the original.
        guard = 0
        while (driver.stackSize > 0 && guard < 20) {
            driver.bothPass()
            guard++
        }
        driver.getLifeTotal(opponent) shouldBe 17
        driver.getLifeTotal(me) shouldBe 17
    }

    test("mode + {1} (change target): redirect a single-target spell") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.passPriority(me)
        // Opponent aims the bolt at me.
        driver.castSpellWithTargets(opponent, bolt, listOf(ChosenTarget.Player(me)))
        val boltOnStack = driver.getTopOfStack()!!
        driver.passPriority(opponent)

        val rtf = driver.putCardInHand(me, "Return the Favor")
        driver.giveMana(me, Color.RED, 2)
        driver.giveColorlessMana(me, 1) // {R}{R} base + {1} for the change-target mode
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = rtf,
                targets = listOf(ChosenTarget.Spell(boltOnStack)),
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Spell(boltOnStack))),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true

        // Resolve Return the Favor → pause to choose the bolt's new (single) target.
        driver.bothPass()

        // Redirect the bolt onto the opponent (change-target uses a card-selection decision).
        driver.submitCardSelection(me, listOf(opponent))

        // Resolve the redirected bolt → opponent takes 3, I take none.
        var guard = 0
        while (driver.stackSize > 0 && guard < 20) {
            driver.bothPass()
            guard++
        }
        driver.getLifeTotal(opponent) shouldBe 17
        driver.getLifeTotal(me) shouldBe 20
    }

    test("mode + {1} (copy): copy a target triggered ability and retarget the copy") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        // I play Zap Beast aimed at the opponent; its ETB trigger goes on the stack targeting them.
        val beast = driver.putCardInHand(me, "Zap Beast")
        driver.giveMana(me, Color.RED, 1)
        driver.giveColorlessMana(me, 1)
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = beast,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass() // Zap Beast resolves → its targeted ETB trigger prompts for a target

        // Choose the ETB trigger's target (the opponent); the trigger then sits on the stack.
        var guard = 0
        while (driver.state.pendingDecision !is ChooseTargetsDecision && guard < 20) {
            driver.bothPass()
            guard++
        }
        (driver.state.pendingDecision is ChooseTargetsDecision) shouldBe true
        driver.submitTargetSelection(me, listOf(opponent)).isSuccess shouldBe true

        val triggerOnStack = driver.getTopOfStack()!!

        // In response to the trigger, I cast Return the Favor copying it, aiming the copy at myself
        // so I can confirm the copy hit a *different* target than the original.
        val rtf = driver.putCardInHand(me, "Return the Favor")
        driver.giveMana(me, Color.RED, 2)
        driver.giveColorlessMana(me, 1)
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = rtf,
                targets = listOf(ChosenTarget.Spell(triggerOnStack)),
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Spell(triggerOnStack))),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true

        // Resolve Return the Favor → pause to choose new targets for the copied ability.
        guard = 0
        while (driver.state.pendingDecision !is ChooseTargetsDecision && driver.stackSize > 0 && guard < 20) {
            driver.bothPass()
            guard++
        }
        (driver.state.pendingDecision is ChooseTargetsDecision) shouldBe true
        driver.submitTargetSelection(me, listOf(me)).isSuccess shouldBe true

        // Resolve the copy (hits me for 2) then the original (hits opponent for 2).
        guard = 0
        while (driver.stackSize > 0 && guard < 30) {
            driver.bothPass()
            guard++
        }
        driver.getLifeTotal(me) shouldBe 18
        driver.getLifeTotal(opponent) shouldBe 18
    }

    test("Spree requires at least one mode: casting with no modes fails") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val rtf = driver.putCardInHand(me, "Return the Favor")
        driver.giveMana(me, Color.RED, 2)
        val result = driver.submit(
            CastSpell(
                playerId = me,
                cardId = rtf,
                chosenModes = emptyList(),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe false
    }
})
