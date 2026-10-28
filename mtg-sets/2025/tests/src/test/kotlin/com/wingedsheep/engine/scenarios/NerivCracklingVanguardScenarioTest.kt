package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Neriv, Crackling Vanguard — "Whenever Neriv attacks, exile a number of cards from the top of
 * your library equal to the number of differently named tokens you control. During any turn you
 * attacked with a commander, you may play those cards."
 *
 * Two Soldier tokens share one name, so one card is exiled. Whether it can be played depends on
 * whether a commander was among the declared attackers this turn.
 */
class NerivCracklingVanguardScenarioTest : FunSpec({

    class Board(val driver: GameTestDriver, val me: EntityId, val opponent: EntityId, val neriv: EntityId, val commander: EntityId, val topCard: EntityId)

    fun setUp(): Board {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val neriv = driver.putCreatureOnBattlefield(me, "Neriv, Crackling Vanguard")
        driver.removeSummoningSickness(neriv)
        val commander = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.removeSummoningSickness(commander)
        driver.replaceState(driver.state.updateEntity(commander) { it.with(CommanderComponent(ownerId = me)) })

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val alarm = driver.putCardInHand(me, "Raise the Alarm")
        driver.giveMana(me, Color.WHITE, 2)
        driver.castSpell(me, alarm)
        driver.bothPass()

        val topCard = driver.putCardOnTopOfLibrary(me, "Mountain")
        return Board(driver, me, opponent, neriv, commander, topCard)
    }

    fun GameTestDriver.landPlays(player: EntityId): List<EntityId> =
        legalActions(player).mapNotNull { (it.action as? PlayLand)?.cardId }

    test("attacking alongside a commander lets you play the exiled card") {
        val b = setUp()
        val driver = b.driver

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(b.me, listOf(b.neriv, b.commander), b.opponent)
        driver.bothPass() // resolve Neriv's attack trigger

        withClue("Two Soldier Tokens are one name, so exactly the top card is exiled") {
            driver.getExile(b.me) shouldBe listOf(b.topCard)
        }

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        withClue("A commander attacked this turn, so the exiled land can be played") {
            driver.landPlays(b.me) shouldContain b.topCard
        }
    }

    test("without a commander attacking, the exiled card can't be played") {
        val b = setUp()
        val driver = b.driver

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(b.me, listOf(b.neriv), b.opponent)
        driver.bothPass()

        driver.getExile(b.me) shouldBe listOf(b.topCard)

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        withClue("Neriv isn't a commander; no commander attacked this turn") {
            driver.landPlays(b.me) shouldNotContain b.topCard
        }
    }
})
