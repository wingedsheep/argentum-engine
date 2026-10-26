package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * A named, non-targeted damage source ("it deals that much damage" — Wrathful Red Dragon,
 * Warstorm Surge) that has left the battlefield before its instruction resolves still deals the
 * damage, using its last-known information (CR 608.2h): the damage happens, and the departed
 * source's lifelink and deathtouch as it last existed on the battlefield still apply.
 */
class DepartedDamageSourceTest : FunSpec({

    // "Whenever a creature you control is dealt damage, it deals that much damage to any target."
    val ReflectionField = card("Reflection Field") {
        manaCost = "{2}{R}"
        typeLine = "Enchantment"
        oracleText = "Whenever a creature you control is dealt damage, it deals that much damage to any target."
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature.youControl()).isDealtDamage()
            val t = target(Targets.Any)
            effect = Effects.DealDamage(
                amount = DynamicAmounts.triggerDamageAmount(),
                target = t,
                damageSource = EffectTarget.TriggeringEntity
            )
        }
    }

    val LifelinkMite = card("Lifelink Mite") {
        manaCost = "{W}"
        typeLine = "Creature — Insect"
        power = 1
        toughness = 1
        keywords(Keyword.LIFELINK)
    }

    val DeathtouchMite = card("Deathtouch Mite") {
        manaCost = "{B}"
        typeLine = "Creature — Insect"
        power = 1
        toughness = 1
        keywords(Keyword.DEATHTOUCH)
    }

    val BigWall = card("Big Wall") {
        manaCost = "{3}"
        typeLine = "Creature — Wall"
        power = 0
        toughness = 8
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ReflectionField, LifelinkMite, DeathtouchMite, BigWall))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Bolt [victim] (3 damage, lethal for a 1/1), then aim the reflected damage at [reflectTarget]. */
    fun boltAndReflect(driver: GameTestDriver, victim: EntityId, reflectTarget: EntityId) {
        val p1 = driver.player1
        driver.giveMana(p1, Color.RED, 1)
        val bolt = driver.putCardInHand(p1, "Lightning Bolt")
        driver.castSpellWithTargets(p1, bolt, listOf(ChosenTarget.Permanent(victim))).error shouldBe null
        var guard = 0
        while (guard++ < 16) {
            when (val decision = driver.state.pendingDecision) {
                is ChooseTargetsDecision -> driver.submitTargetSelection(decision.playerId, listOf(reflectTarget))
                null -> {
                    if (driver.state.stack.isEmpty()) return
                    driver.bothPass()
                }
                else -> driver.autoResolveDecision()
            }
        }
    }

    test("a creature killed by the damage still deals the reflected damage") {
        val driver = newDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Reflection Field")
        val mite = driver.putCreatureOnBattlefield(driver.player1, "Deathtouch Mite")

        boltAndReflect(driver, victim = mite, reflectTarget = driver.player2)

        driver.getGraveyardCardNames(driver.player1) shouldContain "Deathtouch Mite"
        driver.getLifeTotal(driver.player2) shouldBe 17
    }

    test("a departed source's lifelink, as it last existed, gains its controller life") {
        val driver = newDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Reflection Field")
        val mite = driver.putCreatureOnBattlefield(driver.player1, "Lifelink Mite")

        boltAndReflect(driver, victim = mite, reflectTarget = driver.player2)

        driver.getGraveyardCardNames(driver.player1) shouldContain "Lifelink Mite"
        driver.getLifeTotal(driver.player2) shouldBe 17
        driver.getLifeTotal(driver.player1) shouldBe 23
    }

    test("a departed source's deathtouch, as it last existed, destroys the creature it damages") {
        val driver = newDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Reflection Field")
        val mite = driver.putCreatureOnBattlefield(driver.player1, "Deathtouch Mite")
        val wall = driver.putCreatureOnBattlefield(driver.player2, "Big Wall")

        boltAndReflect(driver, victim = mite, reflectTarget = wall)

        driver.getGraveyardCardNames(driver.player2) shouldContain "Big Wall"
    }
})
