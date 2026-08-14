package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.mtg.sets.definitions.fdn.cards.TwinflameTyrant
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Twinflame Tyrant (FDN #97).
 *
 * {3}{R}{R} Creature — Dragon 3/5
 * Flying
 * If a source you control would deal damage to an opponent or a permanent an opponent controls,
 * it deals double that damage instead.
 *
 * Covers every damage path the doubling has to reach — combat damage to the defending player,
 * combat damage to a blocker, noncombat spell damage to a player and to a permanent — plus the
 * scope boundaries (your own permanents and yourself are untouched, an opponent's source is
 * untouched) and the stacking case: two Tyrants are two separate replacement effects, each applied
 * once (CR 616.1), so the damage is quadrupled.
 */
class TwinflameTyrantScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TwinflameTyrant))
        return driver
    }

    /** Put a ready-to-attack Twinflame Tyrant onto [playerId]'s battlefield. */
    fun GameTestDriver.tyrantFor(playerId: EntityId): EntityId {
        val tyrant = putCreatureOnBattlefield(playerId, "Twinflame Tyrant")
        removeSummoningSickness(tyrant)
        return tyrant
    }

    test("combat damage to the defending player is doubled") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val tyrant = driver.tyrantFor(active)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(tyrant), defendingPlayer = opponent).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareNoBlockers(opponent).error shouldBe null
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
        if (driver.pendingDecision is CombatResolutionDecision) {
            driver.confirmCombatDamage()
        }

        withClue("3 power doubled to 6 combat damage") {
            driver.getLifeTotal(opponent) shouldBe 14
        }
    }

    test("combat damage to a creature an opponent controls is doubled") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // The Tyrant stays home — it doubles damage from *any* source you control, and its own
        // flying would otherwise stop the blocker from being declared at all.
        driver.tyrantFor(active)
        val attacker = driver.putCreatureOnBattlefield(active, "Centaur Courser") // 3/3
        driver.removeSummoningSickness(attacker)
        // 12/12 so it survives and keeps the marked damage readable.
        val blocker = driver.putCreatureOnBattlefield(opponent, "Ghalta, Primal Hunger")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(attacker), defendingPlayer = opponent).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(blocker to listOf(attacker))).error shouldBe null
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
        if (driver.pendingDecision is CombatResolutionDecision) {
            driver.confirmCombatDamage()
        }

        withClue("3 power doubled to 6 damage marked on the blocker") {
            driver.state.getEntity(blocker)?.get<DamageComponent>()?.amount shouldBe 6
        }
    }

    test("noncombat spell damage to an opponent is doubled") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.tyrantFor(active)

        val bolt = driver.putCardInHand(active, "Lightning Bolt")
        driver.giveMana(active, Color.RED, 1)
        driver.castSpellWithTargets(
            active,
            bolt,
            listOf(entityIdToChosenTarget(driver.state, opponent)),
        ).error shouldBe null
        driver.bothPass()

        withClue("Lightning Bolt's 3 damage doubled to 6") {
            driver.getLifeTotal(opponent) shouldBe 14
        }
    }

    test("noncombat spell damage to a permanent an opponent controls is doubled") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.tyrantFor(active)
        // 5/5 — survives an unamplified Bolt, dies to the doubled 6.
        val theirCreature = driver.putCreatureOnBattlefield(opponent, "Force of Nature")

        val bolt = driver.putCardInHand(active, "Lightning Bolt")
        driver.giveMana(active, Color.RED, 1)
        driver.castSpellWithTargets(
            active,
            bolt,
            listOf(entityIdToChosenTarget(driver.state, theirCreature)),
        ).error shouldBe null
        driver.bothPass()

        withClue("6 damage is lethal to the 5/5") {
            driver.getGraveyardCardNames(opponent) shouldBe listOf("Force of Nature")
        }
    }

    test("scope: only sources you control, only opponents and their permanents") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.tyrantFor(active)
        val myCreature = driver.putCreatureOnBattlefield(active, "Centaur Courser")
        val myOtherCreature = driver.putCreatureOnBattlefield(active, "Centaur Courser")
        val theirCreature = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        withClue("your source dealing 3 to an opponent → 6") {
            DamageUtils.applyStaticDamageAmplification(
                driver.state, targetId = opponent, amount = 3, sourceId = myCreature
            ) shouldBe 6
        }
        withClue("your source dealing 3 to a permanent an opponent controls → 6") {
            DamageUtils.applyStaticDamageAmplification(
                driver.state, targetId = theirCreature, amount = 3, sourceId = myCreature
            ) shouldBe 6
        }
        withClue("your source dealing 3 to your own creature is NOT doubled") {
            DamageUtils.applyStaticDamageAmplification(
                driver.state, targetId = myOtherCreature, amount = 3, sourceId = myCreature
            ) shouldBe 3
        }
        withClue("your source dealing 3 to you is NOT doubled") {
            DamageUtils.applyStaticDamageAmplification(
                driver.state, targetId = active, amount = 3, sourceId = myCreature
            ) shouldBe 3
        }
        withClue("an opponent's source dealing 3 to you is NOT doubled") {
            DamageUtils.applyStaticDamageAmplification(
                driver.state, targetId = active, amount = 3, sourceId = theirCreature
            ) shouldBe 3
        }
        withClue("an opponent's source dealing 3 to their own creature is NOT doubled") {
            DamageUtils.applyStaticDamageAmplification(
                driver.state, targetId = theirCreature, amount = 3, sourceId = theirCreature
            ) shouldBe 3
        }
    }

    test("the opponent's Twinflame Tyrant doubles damage aimed back at you") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.tyrantFor(opponent)
        val theirCreature = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        val myCreature = driver.putCreatureOnBattlefield(active, "Centaur Courser")

        withClue("their source dealing 3 to you → 6") {
            DamageUtils.applyStaticDamageAmplification(
                driver.state, targetId = active, amount = 3, sourceId = theirCreature
            ) shouldBe 6
        }
        withClue("their source dealing 3 to your creature → 6") {
            DamageUtils.applyStaticDamageAmplification(
                driver.state, targetId = myCreature, amount = 3, sourceId = theirCreature
            ) shouldBe 6
        }
        withClue("your source dealing 3 to them is NOT doubled by their Tyrant") {
            DamageUtils.applyStaticDamageAmplification(
                driver.state, targetId = opponent, amount = 3, sourceId = myCreature
            ) shouldBe 3
        }
    }

    test("two Twinflame Tyrants quadruple the damage (each replacement applies once)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.tyrantFor(active)
        driver.tyrantFor(active)

        val bolt = driver.putCardInHand(active, "Lightning Bolt")
        driver.giveMana(active, Color.RED, 1)
        driver.castSpellWithTargets(
            active,
            bolt,
            listOf(entityIdToChosenTarget(driver.state, opponent)),
        ).error shouldBe null
        driver.bothPass()

        withClue("3 → 6 → 12: each Tyrant doubles once") {
            driver.getLifeTotal(opponent) shouldBe 8
        }
    }

    test("the threatened player gets a Damage Doubled badge, its controller does not") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val transformer = ClientStateTransformer(driver.cardRegistry)
        fun doubledBadgesOn(playerId: EntityId): List<String?> =
            transformer.transform(driver.state, playerId)
                .players.first { it.playerId == playerId }
                .activeEffects.filter { it.effectId.startsWith("damage_doubled") }
                .map { it.description }

        withClue("no Tyrant yet — no badge on either player") {
            doubledBadgesOn(opponent) shouldBe emptyList()
            doubledBadgesOn(active) shouldBe emptyList()
        }

        driver.tyrantFor(active)

        withClue("the opponent is warned") {
            doubledBadgesOn(opponent) shouldBe
                listOf("Damage dealt to you is doubled by Twinflame Tyrant")
        }
        withClue("its own controller is not a legal recipient, so no badge") {
            doubledBadgesOn(active) shouldBe emptyList()
        }

        driver.tyrantFor(active)

        withClue("two Tyrants are two replacements — two badges, x4 damage") {
            doubledBadgesOn(opponent).size shouldBe 2
        }
    }

    test("two Twinflame Tyrants quadruple combat damage too") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.tyrantFor(active)
        driver.tyrantFor(active)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(attacker), defendingPlayer = opponent).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareNoBlockers(opponent).error shouldBe null
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
        if (driver.pendingDecision is CombatResolutionDecision) {
            driver.confirmCombatDamage()
        }

        withClue("3 power → 6 → 12 combat damage") {
            driver.getLifeTotal(opponent) shouldBe 8
        }
    }
})
