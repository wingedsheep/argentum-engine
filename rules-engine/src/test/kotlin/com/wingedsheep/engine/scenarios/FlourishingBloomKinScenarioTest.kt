package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.FlourishingBloomKin
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Flourishing Bloom-Kin (MKM #160) — {1}{G} 0/0 Plant Elemental with Disguise {4}{G}.
 *
 * "This creature gets +1/+1 for each Forest you control.
 *  Disguise {4}{G}
 *  When this creature is turned face up, search your library for up to two Forest cards and reveal
 *  them. Put one of them onto the battlefield tapped and the other into your hand, then shuffle."
 *
 * A printed 0/0 that is alive only because of its own static ability, so the static is the first
 * thing tested: it is a continuous effect recomputed in the projection, which means losing your last
 * Forest kills it on the next state-based-action check. Face down it dodges that entirely — CR 708.2
 * strips the ability along with everything else, and the 2/2 body needs no Forests to survive.
 *
 * The search is the Cultivate split: `ChooseUpTo(2)` for the find, then `ChooseExactly(1)` whose
 * *remainder* goes to hand. The official ruling — find one Forest and it must go to the battlefield,
 * with no option to route it to hand instead — falls out of `ChooseExactly(1)` auto-selecting the
 * only card and leaving an empty remainder, and that is what the last test pins.
 *
 * Unlike [BubbleSmugglerScenarioTest]'s replacement, this is a **triggered** ability: the flip
 * resolves first and the search happens off the stack afterwards.
 */
class FlourishingBloomKinScenarioTest : FunSpec({

    fun createDriver(deck: Deck): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FlourishingBloomKin))
        driver.initMirrorMatch(deck = deck, skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Cast the Bloom-Kin face down for {3} and return the resulting face-down permanent. */
    fun castFaceDown(driver: GameTestDriver, player: EntityId): EntityId {
        val card = driver.putCardInHand(player, "Flourishing Bloom-Kin")
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

    /** Turn it face up for its disguise cost {4}{G}. Does not resolve the resulting trigger. */
    fun flipFaceUp(driver: GameTestDriver, player: EntityId, bloomKin: EntityId) {
        driver.giveColorlessMana(player, 4)
        driver.giveMana(player, Color.GREEN, 1)
        driver.submit(
            TurnFaceUp(
                playerId = player,
                sourceId = bloomKin,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null
    }

    fun castFaceUp(driver: GameTestDriver, player: EntityId): EntityId {
        val card = driver.putCardInHand(player, "Flourishing Bloom-Kin")
        driver.giveColorlessMana(player, 1)
        driver.giveMana(player, Color.GREEN, 1)
        driver.submit(
            CastSpell(playerId = player, cardId = card, paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        driver.bothPass()
        return driver.findPermanent(player, "Flourishing Bloom-Kin")
            ?: error("Flourishing Bloom-Kin did not resolve onto the battlefield")
    }

    test("it is a 0/0 plus one for each Forest you control") {
        val driver = createDriver(Deck.of("Forest" to 40))
        val player = driver.activePlayer!!

        repeat(3) { driver.putLandOnBattlefield(player, "Forest") }
        val bloomKin = castFaceUp(driver, player)

        driver.state.projectedState.getPower(bloomKin) shouldBe 3
        driver.state.projectedState.getToughness(bloomKin) shouldBe 3
    }

    test("only your own Forests count") {
        val driver = createDriver(Deck.of("Forest" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        driver.putLandOnBattlefield(player, "Forest")
        repeat(3) { driver.putLandOnBattlefield(opponent, "Forest") }
        val bloomKin = castFaceUp(driver, player)

        withClue("one Forest of your own — the opponent's three are irrelevant") {
            driver.state.projectedState.getPower(bloomKin) shouldBe 1
            driver.state.projectedState.getToughness(bloomKin) shouldBe 1
        }
    }

    test("with no Forests it is a 0/0 and dies to state-based actions") {
        val driver = createDriver(Deck.of("Plains" to 40))
        val player = driver.activePlayer!!

        val card = driver.putCardInHand(player, "Flourishing Bloom-Kin")
        driver.giveColorlessMana(player, 1)
        driver.giveMana(player, Color.GREEN, 1)
        driver.submit(
            CastSpell(playerId = player, cardId = card, paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        driver.bothPass()

        withClue("a 0/0 with no Forests to prop it up never survives its own arrival") {
            driver.findPermanent(player, "Flourishing Bloom-Kin") shouldBe null
            driver.getGraveyardCardNames(player).contains("Flourishing Bloom-Kin") shouldBe true
        }
    }

    test("face down it is a 2/2 that survives with no Forests at all") {
        val driver = createDriver(Deck.of("Plains" to 40))
        val player = driver.activePlayer!!

        val bloomKin = castFaceDown(driver, player)

        withClue("CR 708.2 — no abilities, so no Forest count and no 0/0 body") {
            driver.state.projectedState.getPower(bloomKin) shouldBe 2
            driver.state.projectedState.getToughness(bloomKin) shouldBe 2
        }
    }

    test("flipping it searches out two Forests — one onto the battlefield tapped, one to hand") {
        val driver = createDriver(Deck.of("Forest" to 40))
        val player = driver.activePlayer!!

        val bloomKin = castFaceDown(driver, player)
        val landsBefore = driver.getLands(player).size
        val handBefore = driver.getHandSize(player)

        flipFaceUp(driver, player, bloomKin)
        driver.state.getEntity(bloomKin)?.get<FaceDownComponent>() shouldBe null

        // The trigger uses the stack, unlike a `disguiseFaceUpEffect` replacement.
        withClue("'When … is turned face up' is a trigger, so it goes on the stack") {
            driver.stackSize shouldBe 1
        }
        driver.bothPass()

        val find = driver.pendingDecision as? SelectCardsDecision
        find.shouldNotBeNull()
        val forests = find.options.take(2)
        driver.submitCardSelection(player, forests).error shouldBe null

        val split = driver.pendingDecision as? SelectCardsDecision
        split.shouldNotBeNull()
        withClue("the split step picks exactly one of the two found cards") {
            split.minSelections shouldBe 1
            split.maxSelections shouldBe 1
        }
        driver.submitCardSelection(player, listOf(forests.first())).error shouldBe null

        withClue("one Forest entered tapped") {
            driver.getLands(player).size shouldBe landsBefore + 1
            driver.isTapped(forests.first()) shouldBe true
        }
        withClue("the other went to hand") {
            driver.getHandSize(player) shouldBe handBefore + 1
            driver.getHand(player).contains(forests[1]) shouldBe true
        }
        withClue("the Bloom-Kin is now a 1/1 off the Forest it just put onto the battlefield") {
            driver.state.projectedState.getPower(bloomKin) shouldBe 1
        }
    }

    test("finding only one Forest puts it onto the battlefield, never into your hand") {
        // A library with exactly one Forest in it: the ruling's case.
        val driver = createDriver(Deck.of("Plains" to 39, "Forest" to 1))
        val player = driver.activePlayer!!

        val bloomKin = castFaceDown(driver, player)
        val landsBefore = driver.getLands(player).size
        val handBefore = driver.getHandSize(player)

        flipFaceUp(driver, player, bloomKin)
        driver.bothPass()

        val find = driver.pendingDecision as? SelectCardsDecision
        find.shouldNotBeNull()
        withClue("only one Forest is findable in the whole library") {
            find.options.size shouldBe 1
        }
        val forest = find.options.single()
        driver.submitCardSelection(player, listOf(forest)).error shouldBe null

        withClue("ChooseExactly(1) auto-selects the only card — no second prompt") {
            driver.pendingDecision shouldBe null
        }
        withClue("it goes onto the battlefield tapped, with nothing left over for hand") {
            driver.getLands(player).size shouldBe landsBefore + 1
            driver.isTapped(forest) shouldBe true
            driver.getHandSize(player) shouldBe handBefore
        }
    }
})
