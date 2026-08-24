package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.drk.cards.FellwarStone
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Fellwar Stone — "{T}: Add one mana of any color that a land an opponent
 * controls could produce."
 *
 * The colour set is derived from the *opponents'* board, so the two things worth pinning are that
 * it tracks that board (Swamp gives black, Island gives blue) and that it ignores the Stone's own
 * controller's lands — a Forest of mine must not make green available. The last case is the one a
 * `LandControllerScope.YOU` slip would fail while both others still passed.
 */
class FellwarStoneScenarioTest : FunSpec({

    val abilityId = FellwarStone.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(FellwarStone)
        return driver
    }

    fun pool(driver: GameTestDriver, player: com.wingedsheep.sdk.model.EntityId): ManaPoolComponent =
        driver.state.getEntity(player)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    test("an opponent's Swamp makes black available") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val stone = driver.putPermanentOnBattlefield(me, "Fellwar Stone")
        driver.putLandOnBattlefield(opponent, "Swamp")

        driver.submit(
            ActivateAbility(me, stone, abilityId, manaColorChoice = Color.BLACK)
        ).isSuccess shouldBe true
        pool(driver, me).black shouldBe 1
    }

    test("an opponent's Island makes blue available") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val stone = driver.putPermanentOnBattlefield(me, "Fellwar Stone")
        driver.putLandOnBattlefield(opponent, "Island")

        driver.submit(
            ActivateAbility(me, stone, abilityId, manaColorChoice = Color.BLUE)
        ).isSuccess shouldBe true
        pool(driver, me).blue shouldBe 1
    }

    test("my own lands don't widen it") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val stone = driver.putPermanentOnBattlefield(me, "Fellwar Stone")
        driver.putLandOnBattlefield(opponent, "Swamp")
        driver.putLandOnBattlefield(me, "Forest")

        driver.submit(ActivateAbility(me, stone, abilityId, manaColorChoice = Color.GREEN))

        withClue("green is what MY land makes; the Stone reads the opponent's board only") {
            pool(driver, me).green shouldBe 0
        }
    }
})
