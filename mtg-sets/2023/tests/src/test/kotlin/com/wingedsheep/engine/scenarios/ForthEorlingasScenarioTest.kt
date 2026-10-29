package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltc.cards.ForthEorlingas
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Forth Eorlingas! (LTC #56) — {X}{R}{W} Sorcery.
 *
 * "Create X 2/2 red Human Knight creature tokens with trample and haste. Whenever one or more
 * creatures you control deal combat damage to one or more players this turn, you become the
 * monarch."
 *
 * The "this turn" trigger is an end-of-turn global ability on the offensive combat-damage batch:
 * one trigger however many Knights connect, and gone once the turn ends.
 */
class ForthEorlingasScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ForthEorlingas))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castForth(driver: GameTestDriver, player: EntityId, x: Int) {
        val card = driver.putCardInHand(player, "Forth Eorlingas!")
        driver.giveMana(player, Color.RED, 1)
        driver.giveMana(player, Color.WHITE, 1 + x)
        driver.castXSpell(player, card, x).error shouldBe null
        driver.bothPass()
    }

    fun knights(driver: GameTestDriver, player: EntityId): List<EntityId> =
        driver.getCreatures(player).filter {
            driver.state.getEntity(it)?.get<CardComponent>()?.typeLine?.subtypes
                ?.map { s -> s.value }?.containsAll(listOf("Human", "Knight")) == true
        }

    test("X hasty trampling Knights; connecting with two of them makes you the monarch once") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        castForth(driver, me, 3)

        val tokens = knights(driver, me)
        tokens.size shouldBe 3
        tokens.forEach {
            driver.state.projectedState.getPower(it) shouldBe 2
            driver.state.projectedState.hasKeyword(it, Keyword.TRAMPLE) shouldBe true
            driver.state.projectedState.hasKeyword(it, Keyword.HASTE) shouldBe true
        }
        driver.state.monarch shouldBe null

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, tokens.take(2), opponent).error shouldBe null
        driver.bothPass()
        driver.declareNoBlockers(opponent)
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)

        // One batch trigger for both Knights, not one per creature.
        driver.state.stack.size shouldBe 1
        driver.bothPass()

        driver.state.monarch shouldBe me
        driver.getLifeTotal(opponent) shouldBe 16
    }

    test("no combat damage, no monarch — and the ability ends with the turn") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        castForth(driver, me, 1)
        driver.state.globalGrantedTriggeredAbilities.size shouldBe 1

        driver.passPriorityUntil(Step.UPKEEP, opponent)

        driver.state.monarch shouldBe null
        driver.state.globalGrantedTriggeredAbilities.shouldBeEmpty()
    }
})
