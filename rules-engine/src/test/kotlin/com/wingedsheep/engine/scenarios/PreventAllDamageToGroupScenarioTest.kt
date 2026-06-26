package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.PreventionScope
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Recipient-group damage prevention — [Effects.PreventAllDamageToGroup] /
 * [com.wingedsheep.sdk.scripting.effects.PreventDamageEffect.recipientGroup].
 *
 * Pins the rules of "prevent all damage that would be dealt to creatures you control this turn":
 *   - prevents both noncombat and combat damage to your creatures,
 *   - protects creatures that come under your control *after* the shield is set (the filter is
 *     re-evaluated at the moment damage would be dealt),
 *   - does not protect you (the player) or your opponents' creatures,
 *   - honours the combat-only scope variant.
 *
 * Exercised with inline single-effect instants so the primitive is tested in isolation; the real
 * card (Summon: Alexander) is covered by [CrystalFragmentsScenarioTest].
 */
class PreventAllDamageToGroupScenarioTest : FunSpec({

    val projector = StateProjector()

    // "Prevent all damage that would be dealt to creatures you control this turn."
    val aegis = card("Test Aegis") {
        manaCost = "{W}"
        typeLine = "Instant"
        spell { effect = Effects.PreventAllDamageToGroup(GroupFilter.AllCreaturesYouControl) }
    }

    // Combat-only variant of the same shield.
    val combatAegis = card("Test Combat Aegis") {
        manaCost = "{W}"
        typeLine = "Instant"
        spell {
            effect = Effects.PreventAllDamageToGroup(
                GroupFilter.AllCreaturesYouControl,
                scope = PreventionScope.CombatOnly,
            )
        }
    }

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(aegis, combatAegis))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    fun GameTestDriver.cast(player: EntityId, name: String) {
        giveMana(player, Color.WHITE, 1)
        val spell = putCardInHand(player, name)
        castSpell(player, spell)
        bothPass()
        resolveStack(this)
    }

    fun GameTestDriver.bolt(player: EntityId, target: ChosenTarget) {
        giveMana(player, Color.RED, 1)
        val b = putCardInHand(player, "Lightning Bolt")
        castSpellWithTargets(player, b, listOf(target))
        bothPass()
        resolveStack(this)
    }

    fun GameTestDriver.markedDamage(id: EntityId): Int =
        state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    test("prevents noncombat damage to your creatures but not to you (the player)") {
        val (driver, you) = newGame()
        val mine = driver.putCreatureOnBattlefield(you, "Centaur Courser") // 3/3
        driver.cast(you, "Test Aegis")

        driver.bolt(you, ChosenTarget.Permanent(mine))
        driver.markedDamage(mine) shouldBe 0
        driver.state.getEntity(mine) shouldNotBe null

        // The player is not a creature you control — still takes the damage.
        val before = driver.getLifeTotal(you)
        driver.bolt(you, ChosenTarget.Player(you))
        driver.getLifeTotal(you) shouldBe before - 3
    }

    test("does not protect opponents' creatures") {
        val (driver, you) = newGame()
        val opponent = driver.state.turnOrder.first { it != you }
        val theirs = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        driver.cast(you, "Test Aegis")

        driver.bolt(you, ChosenTarget.Permanent(theirs))
        driver.state.getBattlefield().contains(theirs) shouldBe false // dies; shield only covers your creatures
    }

    test("protects a creature that comes under your control after the shield is set") {
        val (driver, you) = newGame()
        driver.cast(you, "Test Aegis")
        // Creature enters AFTER the shield — the filter is re-evaluated at damage time, so it is
        // still protected this turn.
        val late = driver.putCreatureOnBattlefield(you, "Centaur Courser")

        driver.bolt(you, ChosenTarget.Permanent(late))
        driver.markedDamage(late) shouldBe 0
        driver.state.getEntity(late) shouldNotBe null
    }

    test("combat-only variant does not prevent noncombat damage") {
        val (driver, you) = newGame()
        val mine = driver.putCreatureOnBattlefield(you, "Centaur Courser") // 3/3
        driver.cast(you, "Test Combat Aegis")

        driver.bolt(you, ChosenTarget.Permanent(mine))
        driver.state.getBattlefield().contains(mine) shouldBe false // noncombat damage not prevented -> dies
    }

    test("prevents combat damage to your creatures") {
        val (driver, you) = newGame()
        val opponent = driver.state.turnOrder.first { it != you }
        val attacker = driver.putCreatureOnBattlefield(you, "Centaur Courser") // 3/3 (yours)
        val blocker = driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3 (theirs)
        driver.removeSummoningSickness(attacker)
        driver.cast(you, "Test Aegis")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(attacker), defendingPlayer = opponent).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(blocker to listOf(attacker)))
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
        resolveStack(driver)

        // Your attacker takes no combat damage (shielded); the opponent's blocker takes 3 and dies.
        driver.markedDamage(attacker) shouldBe 0
        driver.state.getBattlefield().contains(attacker) shouldBe true
        driver.state.getBattlefield().contains(blocker) shouldBe false
        projector.project(driver.state) // sanity: projection still resolves
    }
})
