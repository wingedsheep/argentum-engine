package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Glóin the Mighty // Easy Pickings (HOB #99) — {3}{R} Legendary Creature — Dwarf Warrior 4/3.
 *
 *   At the beginning of your first main phase, add {R}{R}.
 *
 *   Adventure — Easy Pickings {2}{R}, Sorcery:
 *   Easy Pickings deals 1 damage to each creature your opponents control.
 *
 * Two claims worth pinning: the mana trigger fires on the controller's *precombat* main (and puts
 * two red mana in the pool, not one or a generic two), and the Adventure is one-sided — it must
 * leave the caster's own creatures untouched.
 */
class GloinTheMightyScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        return driver
    }

    fun pool(driver: GameTestDriver, player: EntityId): ManaPoolComponent =
        driver.state.getEntity(player)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    fun resolveStack(driver: GameTestDriver) {
        var safety = 0
        while (driver.stackSize > 0 && safety < 20) {
            driver.bothPass()
            safety++
        }
    }

    test("the beginning-of-first-main trigger adds {R}{R} to its controller's pool") {
        val driver = createDriver()
        val me = driver.player1

        // Onto the battlefield during the upkeep, so the precombat main trigger still has a chance
        // to fire this turn.
        driver.putPermanentOnBattlefield(me, "Glóin the Mighty")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        resolveStack(driver)

        val mana = pool(driver, me)
        withClue("two red, and nothing else — {R}{R} is not {2} and not one mana") {
            mana.red shouldBe 2
            mana.colorless shouldBe 0
            mana.green shouldBe 0
        }
        withClue("the mana goes to Glóin's controller, not their opponent") {
            pool(driver, driver.player2).red shouldBe 0
        }
    }

    test("Easy Pickings damages every creature opponents control and none of your own") {
        val driver = createDriver()
        val me = driver.player1
        val opponent = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val myElf = driver.putCreatureOnBattlefield(me, "Llanowar Elves")        // 1/1 — would die
        val theirElf = driver.putCreatureOnBattlefield(opponent, "Llanowar Elves") // 1/1 — dies
        val theirCourser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3 — lives

        val gloin = driver.putCardInHand(me, "Glóin the Mighty")
        driver.giveMana(me, Color.RED, 3)

        // faceIndex 0 is the Adventure face (Easy Pickings), not the creature.
        driver.submitSuccess(
            CastSpell(
                playerId = me,
                cardId = gloin,
                faceIndex = 0,
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        resolveStack(driver)

        withClue("your opponent's 1/1 dies") {
            driver.getPermanents(opponent).contains(theirElf) shouldBe false
        }
        withClue("your opponent's 3/3 survives 1 damage") {
            driver.getPermanents(opponent) shouldContain theirCourser
        }
        withClue("'creatures your opponents control' must not include your own 1/1") {
            driver.getPermanents(me) shouldContain myElf
        }
        withClue("CR 715: the Adventure exiles itself on resolution, castable later as the creature") {
            driver.getExile(me) shouldContain gloin
        }
    }
})
