package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.DrownInTheLoch
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Drown in the Loch — "Choose one — • Counter target spell with mana value less than or equal to
 * the number of cards in its controller's graveyard. • Destroy target creature with mana value less
 * than or equal to the number of cards in its controller's graveyard."
 *
 * The bound is read per candidate, against *that object's* controller's graveyard — never the
 * caster's. Each test stacks the caster's graveyard against the answer so a cap that read "your
 * graveyard" would flip the outcome.
 */
class DrownInTheLochScenarioTest : FunSpec({

    val counterMode = 0
    val destroyMode = 1

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DrownInTheLoch))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.fillGraveyard(player: EntityId, count: Int) =
        repeat(count) { putCardInGraveyard(player, "Island") }

    fun GameTestDriver.castDrown(caster: EntityId, mode: Int, target: ChosenTarget): ExecutionResult {
        giveMana(caster, Color.BLUE, 1)
        giveMana(caster, Color.BLACK, 1)
        val spell = putCardInHand(caster, "Drown in the Loch")
        return submit(
            CastSpell(
                playerId = caster,
                cardId = spell,
                targets = listOf(target),
                chosenModes = listOf(mode),
                modeTargetsOrdered = listOf(listOf(target)),
            )
        )
    }

    test("destroys an opponent's creature whose mana value fits its controller's graveyard") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val bears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
        driver.fillGraveyard(opp, 2)

        driver.castDrown(me, destroyMode, ChosenTarget.Permanent(bears)).error shouldBe null
        driver.bothPass()

        withClue("Grizzly Bears (MV 2) died to an opponent's 2-card graveyard") {
            driver.findPermanent(opp, "Grizzly Bears") shouldBe null
        }
    }

    test("the caster's own graveyard doesn't raise the cap on an opponent's creature") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val bears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
        driver.fillGraveyard(opp, 1)
        driver.fillGraveyard(me, 6)

        withClue("MV 2 against the opponent's 1-card graveyard is an illegal target") {
            driver.castDrown(me, destroyMode, ChosenTarget.Permanent(bears)).error shouldNotBe null
        }
        driver.findPermanent(opp, "Grizzly Bears") shouldBe bears
    }

    test("each candidate is measured against its own controller's graveyard") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val mine = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.fillGraveyard(opp, 3)

        withClue("my Grizzly Bears is checked against my empty graveyard, not the opponent's") {
            driver.castDrown(me, destroyMode, ChosenTarget.Permanent(mine)).error shouldNotBe null
        }
    }

    test("counters a spell whose mana value fits its caster's graveyard") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val lions = driver.putCreatureOnBattlefield(me, "Savannah Lions")
        driver.fillGraveyard(opp, 1)

        driver.giveMana(opp, Color.RED, 1)
        val bolt = driver.putCardInHand(opp, "Lightning Bolt")
        driver.passPriority(me)
        driver.submit(CastSpell(playerId = opp, cardId = bolt, targets = listOf(ChosenTarget.Permanent(lions))))
            .error shouldBe null
        driver.passPriority(opp)

        val boltOnStack = driver.getTopOfStack()!!
        driver.castDrown(me, counterMode, ChosenTarget.Spell(boltOnStack)).error shouldBe null
        driver.bothPass()
        driver.bothPass()

        withClue("Lightning Bolt was countered, so Savannah Lions survived") {
            driver.findPermanent(me, "Savannah Lions") shouldBe lions
        }
        driver.assertInGraveyard(opp, "Lightning Bolt")
    }

    test("can't counter a spell when its caster's graveyard is too small, however full mine is") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val lions = driver.putCreatureOnBattlefield(me, "Savannah Lions")
        driver.fillGraveyard(me, 6)

        driver.giveMana(opp, Color.RED, 1)
        val bolt = driver.putCardInHand(opp, "Lightning Bolt")
        driver.passPriority(me)
        driver.submit(CastSpell(playerId = opp, cardId = bolt, targets = listOf(ChosenTarget.Permanent(lions))))
            .error shouldBe null
        driver.passPriority(opp)

        val boltOnStack = driver.getTopOfStack()!!
        withClue("Lightning Bolt (MV 1) against the opponent's empty graveyard is an illegal target") {
            driver.castDrown(me, counterMode, ChosenTarget.Spell(boltOnStack)).error shouldNotBe null
        }
    }
})
