package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.SeismicWave
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Seismic Wave: {2}{R} Instant
 * Seismic Wave deals 2 damage to any target and 1 damage to each nonartifact creature target
 * opponent controls.
 *
 * The 1-damage sweep is bound to the opponent *target* by reference. These tests pin that the
 * "any target" slot — which may itself be a player, even the caster — never redirects the sweep,
 * and that artifact creatures are spared.
 */
class SeismicWaveScenarioTest : FunSpec({

    data class Setup(val driver: GameTestDriver, val caster: EntityId, val opponent: EntityId)

    fun setup(): Setup {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SeismicWave))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return Setup(driver, caster, opponent)
    }

    fun GameTestDriver.wave(caster: EntityId, anyTarget: ChosenTarget, opponent: EntityId) {
        val spell = putCardInHand(caster, "Seismic Wave")
        giveMana(caster, Color.RED, 3)
        castSpellWithTargets(caster, spell, listOf(anyTarget, ChosenTarget.Player(opponent))).error shouldBe null
        bothPass()
    }

    fun GameTestDriver.damageOn(id: EntityId) = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0
    fun GameTestDriver.onBattlefield(id: EntityId) = id in state.getBattlefield()

    test("2 to the opponent's face, 1 to each of their nonartifact creatures, artifact creatures spared") {
        val (driver, caster, opponent) = setup()
        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3
        val lions = driver.putCreatureOnBattlefield(opponent, "Savannah Lions") // 2/1
        val construct = driver.putCreatureOnBattlefield(opponent, "Artifact Creature") // 2/2 artifact
        val mine = driver.putCreatureOnBattlefield(caster, "Centaur Courser")

        driver.wave(caster, ChosenTarget.Player(opponent), opponent)

        driver.getLifeTotal(opponent) shouldBe 18
        driver.getLifeTotal(caster) shouldBe 20
        driver.damageOn(courser) shouldBe 1
        driver.onBattlefield(lions) shouldBe false
        driver.damageOn(construct) shouldBe 0
        driver.damageOn(mine) shouldBe 0
    }

    test("targeting yourself with the 2 damage still sweeps only the targeted opponent's creatures") {
        val (driver, caster, opponent) = setup()
        val theirs = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        val mine = driver.putCreatureOnBattlefield(caster, "Centaur Courser")

        driver.wave(caster, ChosenTarget.Player(caster), opponent)

        driver.getLifeTotal(caster) shouldBe 18
        driver.getLifeTotal(opponent) shouldBe 20
        driver.damageOn(theirs) shouldBe 1
        driver.damageOn(mine) shouldBe 0
    }

    test("a creature that is both the any target and in the sweep takes 3") {
        val (driver, caster, opponent) = setup()
        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3

        driver.wave(caster, ChosenTarget.Permanent(courser), opponent)

        driver.onBattlefield(courser) shouldBe false
        driver.getLifeTotal(opponent) shouldBe 20
    }
})
