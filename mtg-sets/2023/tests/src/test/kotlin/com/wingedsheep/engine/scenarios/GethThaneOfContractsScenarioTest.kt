package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.GethThaneOfContracts
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Geth, Thane of Contracts (ONE #95).
 *
 * Other creatures you control get -1/-1.
 * {1}{B}{B}, {T}: Return target creature card from your graveyard to the battlefield. It gains
 * "If this creature would leave the battlefield, exile it instead of putting it anywhere else."
 * Activate only as a sorcery.
 */
class GethThaneOfContractsScenarioTest : FunSpec({

    val returnAbilityId = GethThaneOfContracts.activatedAbilities[0].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(GethThaneOfContracts))
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        return driver
    }

    test("other creatures you control get -1/-1; Geth and opponents' creatures do not") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val geth = driver.putPermanentOnBattlefield(you, "Geth, Thane of Contracts")
        val myBears = driver.putPermanentOnBattlefield(you, "Grizzly Bears")
        val theirBears = driver.putPermanentOnBattlefield(opponent, "Grizzly Bears")

        val projected = driver.state.projectedState
        projected.getPower(geth) shouldBe 3
        projected.getToughness(geth) shouldBe 4
        projected.getPower(myBears) shouldBe 1
        projected.getToughness(myBears) shouldBe 1
        projected.getPower(theirBears) shouldBe 2
        projected.getToughness(theirBears) shouldBe 2
    }

    test("returns target creature card from your graveyard to the battlefield") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val geth = driver.putPermanentOnBattlefield(you, "Geth, Thane of Contracts")
        val bears = driver.putCardInGraveyard(you, "Grizzly Bears")

        driver.giveMana(you, Color.BLACK, 2)
        driver.giveColorlessMana(you, 1)
        driver.submit(
            ActivateAbility(you, geth, returnAbilityId, targets = listOf(ChosenTarget.Card(bears, you, Zone.GRAVEYARD)))
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.state.getBattlefield() shouldContain bears
        driver.state.projectedState.getPower(bears) shouldBe 1
        driver.getGraveyard(you) shouldNotContain bears
    }

    test("a returned X/1 dies to Geth's -1/-1 and is exiled instead of going to the graveyard") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val geth = driver.putPermanentOnBattlefield(you, "Geth, Thane of Contracts")
        val lions = driver.putCardInGraveyard(you, "Savannah Lions")

        driver.giveMana(you, Color.BLACK, 2)
        driver.giveColorlessMana(you, 1)
        driver.submit(
            ActivateAbility(you, geth, returnAbilityId, targets = listOf(ChosenTarget.Card(lions, you, Zone.GRAVEYARD)))
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.state.getBattlefield() shouldNotContain lions
        driver.getGraveyard(you) shouldNotContain lions
        driver.getExile(you) shouldContain lions
    }
})
