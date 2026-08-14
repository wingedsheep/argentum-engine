package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Smaug, Wicked Worm (HOB #164) — {3}{B}{R} Legendary Creature — Dragon 5/5, flying.
 *
 *   When Smaug enters, create X tapped Treasure tokens, where X is the number of artifacts your
 *   opponents control.
 *   Whenever you cast a spell, if mana from a Treasure was spent to cast it, you draw a card and
 *   lose 1 life.
 *
 * Both abilities are easy to get subtly wrong in the same direction: X must count *opponents'*
 * artifacts (not yours, and not everyone's), and the cast trigger must read the spent-mana
 * provenance rather than firing on every spell while you happen to control a Treasure.
 */
class SmaugWickedWormScenarioTest : FunSpec({

    val treasureManaAbilityId = PredefinedTokens.Treasure.activatedAbilities.single().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(PredefinedTokens.Treasure)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun resolveStack(driver: GameTestDriver) {
        var safety = 0
        while (driver.stackSize > 0 && safety < 20) {
            driver.bothPass()
            safety++
        }
    }

    fun treasures(driver: GameTestDriver, player: EntityId): List<EntityId> =
        driver.getPermanents(player).filter { driver.getCardName(it) == "Treasure" }

    test("the enter trigger creates one tapped Treasure per artifact your opponents control") {
        val driver = createDriver()
        val me = driver.player1
        val opponent = driver.player2

        // Two artifacts on their side; one on mine, which must not be counted.
        driver.putCreatureOnBattlefield(opponent, "Frogmite")
        driver.putCreatureOnBattlefield(opponent, "Frogmite")
        driver.putCreatureOnBattlefield(me, "Frogmite")

        val smaug = driver.putCardInHand(me, "Smaug, Wicked Worm")
        driver.giveMana(me, Color.BLACK, 3)
        driver.giveMana(me, Color.RED, 2)
        driver.submitSuccess(
            CastSpell(playerId = me, cardId = smaug, paymentStrategy = PaymentStrategy.FromPool)
        )
        resolveStack(driver)

        val created = treasures(driver, me)
        withClue("X = 2 (their artifacts), not 3 (everyone's) and not 1 (mine)") {
            created.size shouldBe 2
        }
        withClue("the tokens enter tapped") {
            created.all { driver.isTapped(it) } shouldBe true
        }
        withClue("the Treasures belong to Smaug's controller") {
            treasures(driver, opponent).size shouldBe 0
        }
    }

    test("no opponent artifacts means no Treasures — X = 0 creates nothing") {
        val driver = createDriver()
        val me = driver.player1

        val smaug = driver.putCardInHand(me, "Smaug, Wicked Worm")
        driver.giveMana(me, Color.BLACK, 3)
        driver.giveMana(me, Color.RED, 2)
        driver.submitSuccess(
            CastSpell(playerId = me, cardId = smaug, paymentStrategy = PaymentStrategy.FromPool)
        )
        resolveStack(driver)

        treasures(driver, me).size shouldBe 0
    }

    test("a spell paid with Treasure mana draws a card and loses 1 life; the same spell paid from a land does not") {
        val driver = createDriver()
        val me = driver.player1
        val opponent = driver.player2

        driver.putPermanentOnBattlefield(me, "Smaug, Wicked Worm")
        resolveStack(driver)

        // Control: a Bolt paid with ordinary mana, while a Treasure sits untouched on the
        // battlefield. Controlling a Treasure is not the same as spending its mana.
        driver.putPermanentOnBattlefield(me, "Treasure")
        val lifeBefore = driver.getLifeTotal(me)
        val handBefore = driver.getHandSize(me)

        val plainBolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.submitSuccess(
            CastSpell(
                playerId = me,
                cardId = plainBolt,
                targets = listOf(ChosenTarget.Player(opponent)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        resolveStack(driver)

        withClue("no Treasure mana was spent → no life loss and no extra card") {
            driver.getLifeTotal(me) shouldBe lifeBefore
            // +1 for the Bolt arriving in hand, -1 for casting it: back where we started.
            driver.getHandSize(me) shouldBe handBefore
        }

        // The real case: tap and sacrifice the Treasure for {R}, then spend exactly that.
        val treasure = treasures(driver, me).single()
        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = treasure,
                abilityId = treasureManaAbilityId,
                manaColorChoice = Color.RED,
            )
        )
        resolveStack(driver)

        val lifeBeforeTreasureBolt = driver.getLifeTotal(me)
        val handBeforeTreasureBolt = driver.getHandSize(me)

        val treasureBolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.submitSuccess(
            CastSpell(
                playerId = me,
                cardId = treasureBolt,
                targets = listOf(ChosenTarget.Player(opponent)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        resolveStack(driver)

        withClue("Treasure mana was spent → you draw a card and lose 1 life") {
            driver.getLifeTotal(me) shouldBe lifeBeforeTreasureBolt - 1
            // +1 for the Bolt going into hand, -1 for casting it, +1 for the trigger's draw.
            driver.getHandSize(me) shouldBe handBeforeTreasureBolt + 1 - 1 + 1
        }
    }
})
