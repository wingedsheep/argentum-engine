package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.CrowdControlWarden
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Crowd-Control Warden (MKM #193) — {3}{G}{W} 4/4 Centaur Soldier with Disguise {3}{G/W}{G/W}.
 *
 * "As this creature enters or is turned face up, put X +1/+1 counters on it, where X is the number
 *  of other creatures you control."
 *
 * One sentence, two different rules constructs, and the whole point of these tests is that both
 * arrive at the *same* number by opposite routes:
 *
 *  - **Entering** is a CR 614.1c enters-with-counters replacement. The Warden isn't on the
 *    battlefield yet when the count runs, so a plain "creatures you control" tally is already
 *    "other creatures you control".
 *  - **Turning face up** is a replacement riding the turn-up special action, and there the Warden
 *    *is* on the battlefield — so it needs `excludeSelf`. Without it the flip pays out one counter
 *    too many, which is the off-by-one these tests exist to catch.
 *
 * Also pinned: only *your* creatures count, and a Warden that enters face down gets nothing at all
 * (CR 708.2 — a face-down permanent has no abilities, so neither half can apply).
 */
class CrowdControlWardenScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(CrowdControlWarden))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun counterCount(driver: GameTestDriver, entityId: EntityId): Int =
        driver.state.getEntity(entityId)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Cast the Warden face down for {3} and return the resulting face-down permanent. */
    fun castFaceDown(driver: GameTestDriver, player: EntityId): EntityId {
        val card = driver.putCardInHand(player, "Crowd-Control Warden")
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

    /** Turn a face-down Warden face up for its disguise cost {3}{G/W}{G/W}. */
    fun flipFaceUp(driver: GameTestDriver, player: EntityId, warden: EntityId) {
        driver.giveColorlessMana(player, 3)
        driver.giveMana(player, Color.GREEN, 2)
        driver.submit(
            TurnFaceUp(
                playerId = player,
                sourceId = warden,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null
    }

    test("entering face up counts the other creatures you control, not itself") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.putCreatureOnBattlefield(player, "Centaur Courser")

        val card = driver.putCardInHand(player, "Crowd-Control Warden")
        driver.giveColorlessMana(player, 3)
        driver.giveMana(player, Color.GREEN, 1)
        driver.giveMana(player, Color.WHITE, 1)
        driver.submit(
            CastSpell(playerId = player, cardId = card, paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        driver.bothPass()

        val warden = driver.findPermanent(player, "Crowd-Control Warden").shouldNotBeNull()
        withClue("two other creatures — the Warden must not count itself") {
            counterCount(driver, warden) shouldBe 2
        }
        driver.state.projectedState.getPower(warden) shouldBe 6
        driver.state.projectedState.getToughness(warden) shouldBe 6
    }

    test("entering with no other creatures puts no counters on it") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val card = driver.putCardInHand(player, "Crowd-Control Warden")
        driver.giveColorlessMana(player, 3)
        driver.giveMana(player, Color.GREEN, 1)
        driver.giveMana(player, Color.WHITE, 1)
        driver.submit(
            CastSpell(playerId = player, cardId = card, paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        driver.bothPass()

        val warden = driver.findPermanent(player, "Crowd-Control Warden").shouldNotBeNull()
        counterCount(driver, warden) shouldBe 0
        driver.state.projectedState.getPower(warden) shouldBe 4
        driver.state.projectedState.getToughness(warden) shouldBe 4
    }

    test("the opponent's creatures never count") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        val card = driver.putCardInHand(player, "Crowd-Control Warden")
        driver.giveColorlessMana(player, 3)
        driver.giveMana(player, Color.GREEN, 1)
        driver.giveMana(player, Color.WHITE, 1)
        driver.submit(
            CastSpell(playerId = player, cardId = card, paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        driver.bothPass()

        val warden = driver.findPermanent(player, "Crowd-Control Warden").shouldNotBeNull()
        withClue("only the one creature you control counts") {
            counterCount(driver, warden) shouldBe 1
        }
    }

    test("cast face down it is a vanilla 2/2 — neither half of the ability applies") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.putCreatureOnBattlefield(player, "Centaur Courser")

        val warden = castFaceDown(driver, player)

        withClue("CR 708.2 — a face-down permanent has no abilities") {
            counterCount(driver, warden) shouldBe 0
        }
        driver.state.projectedState.getPower(warden) shouldBe 2
        driver.state.projectedState.getToughness(warden) shouldBe 2
    }

    test("turning it face up excludes itself from the count — the off-by-one guard") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.putCreatureOnBattlefield(player, "Centaur Courser")

        val warden = castFaceDown(driver, player)
        // Face down the Warden is itself a creature on the battlefield. A count without
        // `excludeSelf` would see three creatures and hand out three counters.
        flipFaceUp(driver, player, warden)

        driver.state.getEntity(warden)?.get<FaceDownComponent>() shouldBe null
        withClue("two other creatures, so exactly two counters — not three") {
            counterCount(driver, warden) shouldBe 2
        }
        driver.state.projectedState.getPower(warden) shouldBe 6
        driver.state.projectedState.getToughness(warden) shouldBe 6
    }

    test("the flip payout lands inside the special action — nothing uses the stack") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val warden = castFaceDown(driver, player)

        flipFaceUp(driver, player, warden)

        // A replacement riding the turn-up procedure, not a triggered ability: the opponent never
        // gets priority with a counter-less 4/4 Warden on the battlefield.
        driver.stackSize shouldBe 0
        driver.pendingDecision shouldBe null
        counterCount(driver, warden) shouldBe 1
    }

    test("turning it face up with no other creatures puts no counters on it") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val warden = castFaceDown(driver, player)
        flipFaceUp(driver, player, warden)

        withClue("the Warden is the only creature, and it doesn't count itself") {
            counterCount(driver, warden) shouldBe 0
        }
        driver.state.projectedState.getPower(warden) shouldBe 4
        driver.state.projectedState.getToughness(warden) shouldBe 4
    }
})
