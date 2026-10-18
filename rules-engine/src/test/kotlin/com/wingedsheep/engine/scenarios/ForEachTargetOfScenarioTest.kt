package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Feature test for `IterationSpace.TargetsOf` (`Effects.ForEachTargetOf`) — iterate the targets of
 * one declaration while the spell has others.
 *
 * The inline sorcery declares its X victims *before* the creature that deals the damage, so the
 * later declaration only binds if the X group is narrowed to the targets actually chosen.
 */
class ForEachTargetOfScenarioTest : FunSpec({

    val volley = card("Test Backwards Volley") {
        manaCost = "{X}{R}"
        typeLine = "Sorcery"
        oracleText = "Each of X target creatures is dealt damage equal to the power of target creature you control by that creature."
        spell {
            val victims = targets(TargetFilter.Creature, exactly = DynamicAmounts.xValue())
            val source = target(TargetFilter.CreatureYouControl)
            effect = Effects.ForEachTargetOf(
                victims,
                Effects.DealDamage(DynamicAmounts.powerOf(source), EffectTarget.IterationEntity, damageSource = source)
            )
        }
    }

    data class Setup(val driver: GameTestDriver, val caster: EntityId, val opponent: EntityId)

    fun setup(): Setup {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(volley))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val caster = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return Setup(driver, caster, driver.getOpponent(caster))
    }

    fun GameTestDriver.damageOn(id: EntityId) = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    fun GameTestDriver.volley(caster: EntityId, x: Int, targets: List<EntityId>) {
        val spell = putCardInHand(caster, "Test Backwards Volley")
        giveMana(caster, Color.RED, 1 + x)
        castXSpell(caster, spell, x, targets).error shouldBe null
    }

    test("a variable group declared first iterates only its own targets; the later declaration still binds") {
        val (driver, caster, opponent) = setup()
        val first = driver.putCreatureOnBattlefield(opponent, "Force of Nature")
        val second = driver.putCreatureOnBattlefield(opponent, "Force of Nature")
        val courser = driver.putCreatureOnBattlefield(caster, "Centaur Courser")

        driver.volley(caster, x = 2, targets = listOf(first, second, courser))
        driver.bothPass()

        driver.damageOn(first) shouldBe 3
        driver.damageOn(second) shouldBe 3
        driver.damageOn(courser) shouldBe 0
    }

    test("a victim that became illegal is skipped, even the first slot of the group") {
        val (driver, caster, opponent) = setup()
        val gone = driver.putCreatureOnBattlefield(opponent, "Force of Nature")
        val stays = driver.putCreatureOnBattlefield(opponent, "Force of Nature")
        val courser = driver.putCreatureOnBattlefield(caster, "Centaur Courser")

        driver.volley(caster, x = 2, targets = listOf(gone, stays, courser))
        driver.moveToGraveyard(gone)
        driver.bothPass()

        driver.damageOn(stays) shouldBe 3
        driver.damageOn(courser) shouldBe 0
    }

    test("X = 0 iterates nothing") {
        val (driver, caster, opponent) = setup()
        val bystander = driver.putCreatureOnBattlefield(opponent, "Force of Nature")
        val courser = driver.putCreatureOnBattlefield(caster, "Centaur Courser")

        driver.volley(caster, x = 0, targets = listOf(courser))
        driver.bothPass()

        driver.damageOn(bystander) shouldBe 0
        driver.damageOn(courser) shouldBe 0
    }
})
