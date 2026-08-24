package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.AureliasVindicator
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Aurelia's Vindicator (MKM) — {2}{W}{W} 4/2 Angel with flying, lifelink, ward {2} and
 * Disguise {X}{3}{W}.
 *
 * "When this creature is turned face up, exile up to X other target creatures from the battlefield
 *  and/or creature cards from graveyards."
 * "When this creature leaves the battlefield, return the exiled cards to their owners' hands."
 *
 * This is the only card in Magic whose X is chosen inside a *turn-up* cost, so these tests pin the
 * whole path the X takes — `TurnFaceUp.xValue` → `TurnFaceUpEvent` → the trigger's stack object →
 * `DynamicAmount.XValue` as the target-count cap — plus the fact that the pool is actually charged
 * for it, which it previously was not.
 */
class AureliasVindicatorScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(AureliasVindicator))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Cast the Vindicator face down for {3} and return the resulting face-down permanent. */
    fun castFaceDown(driver: GameTestDriver, player: EntityId): EntityId {
        val card = driver.putCardInHand(player, "Aurelia's Vindicator")
        driver.giveColorlessMana(player, 3)
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = card,
                castFaceDown = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null
        driver.bothPass()
        return driver.getPermanents(player).single {
            driver.state.getEntity(it)?.has<FaceDownComponent>() == true
        }
    }

    /** Mana for `Disguise {X}{3}{W}` at the given X: {3} + {W} + X generic. */
    fun fundFlip(driver: GameTestDriver, player: EntityId, x: Int) {
        driver.giveColorlessMana(player, 3 + x)
        driver.giveMana(player, Color.WHITE, 1)
    }

    test("turned face up for X=2 it exiles two targets — one on the battlefield, one in a graveyard") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        val lions = driver.putCardInGraveyard(opponent, "Savannah Lions")

        val vindicator = castFaceDown(driver, player)
        fundFlip(driver, player, x = 2)

        driver.submit(
            TurnFaceUp(
                playerId = player,
                sourceId = vindicator,
                paymentStrategy = PaymentStrategy.FromPool,
                xValue = 2
            )
        ).error shouldBe null

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseTargetsDecision>()

        withClue("the union offers both the battlefield creature and the graveyard creature card") {
            val offered = decision.legalTargets[0].orEmpty()
            offered shouldContain courser
            offered shouldContain lions
        }

        driver.submitTargetSelection(player, listOf(courser, lions)).error shouldBe null
        repeat(2) { if (!driver.isPaused && driver.stackSize > 0) driver.bothPass() }

        withClue("both chosen objects are exiled, from either zone") {
            driver.getExile(opponent) shouldContainExactlyInAnyOrder listOf(courser, lions)
        }
        withClue("face up it is the printed 4/2 flier") {
            driver.state.projectedState.getPower(vindicator) shouldBe 4
            driver.state.projectedState.getToughness(vindicator) shouldBe 2
            driver.state.projectedState.hasKeyword(vindicator, Keyword.FLYING) shouldBe true
            driver.state.projectedState.hasKeyword(vindicator, Keyword.LIFELINK) shouldBe true
        }
    }

    test("X caps the target count — flipping for X=1 allows only one target") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        driver.putCreatureOnBattlefield(opponent, "Savannah Lions")

        val vindicator = castFaceDown(driver, player)
        fundFlip(driver, player, x = 1)

        driver.submit(
            TurnFaceUp(
                playerId = player,
                sourceId = vindicator,
                paymentStrategy = PaymentStrategy.FromPool,
                xValue = 1
            )
        ).error shouldBe null

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseTargetsDecision>()

        withClue("DynamicAmount.XValue clamps the requirement to the X actually paid") {
            decision.targetRequirements[0].maxTargets shouldBe 1
        }

        driver.submitTargetSelection(player, listOf(courser)).error shouldBe null
        repeat(2) { if (!driver.isPaused && driver.stackSize > 0) driver.bothPass() }

        driver.getExile(opponent) shouldBe listOf(courser)
    }

    test("the pool is charged for X — X=2 on only enough mana for {3}{W} is rejected") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putCreatureOnBattlefield(driver.getOpponent(player), "Centaur Courser")

        val vindicator = castFaceDown(driver, player)
        // Exactly {3}{W}: enough for the printed pips, nothing for X.
        fundFlip(driver, player, x = 0)

        withClue("{X} carries no mana of its own, so the pool must be charged X separately") {
            driver.submit(
                TurnFaceUp(
                    playerId = player,
                    sourceId = vindicator,
                    paymentStrategy = PaymentStrategy.FromPool,
                    xValue = 2
                )
            ).error shouldNotBe null
        }
        withClue("the rejected flip leaves it face down") {
            driver.state.getEntity(vindicator)?.has<FaceDownComponent>() shouldBe true
        }
    }

    test("when it leaves the battlefield the exiled cards go to their owners' hands") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        val lions = driver.putCardInGraveyard(opponent, "Savannah Lions")

        val vindicator = castFaceDown(driver, player)
        fundFlip(driver, player, x = 2)
        driver.submit(
            TurnFaceUp(
                playerId = player,
                sourceId = vindicator,
                paymentStrategy = PaymentStrategy.FromPool,
                xValue = 2
            )
        ).error shouldBe null
        driver.submitTargetSelection(player, listOf(courser, lions)).error shouldBe null
        repeat(2) { if (!driver.isPaused && driver.stackSize > 0) driver.bothPass() }
        driver.getExile(opponent) shouldContainExactlyInAnyOrder listOf(courser, lions)

        // Bolt it: 3 damage kills the 4/2 and fires the leaves-the-battlefield trigger.
        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        driver.giveMana(player, Color.RED, 1)
        driver.castSpellWithTargets(
            player,
            bolt,
            listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(vindicator))
        ).error shouldBe null
        repeat(3) { if (!driver.isPaused && driver.stackSize > 0) driver.bothPass() }

        withClue("both exiled cards return to their owner's hand, not to the battlefield") {
            driver.getHand(opponent) shouldContain courser
            driver.getHand(opponent) shouldContain lions
        }
        withClue("nothing is left in exile") {
            driver.getExile(opponent).none { it == courser || it == lions } shouldBe true
        }
        withClue("the Vindicator itself is in its owner's graveyard") {
            driver.findCardInHand(opponent, "Aurelia's Vindicator") shouldBe null
        }
    }
})
