package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.TheRingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.FrodoSauronsBane
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Frodo, Sauron's Bane (LTR #18) — the level-up Halfling who climbs Citizen → Scout → Rogue.
 *
 * Exercises:
 *  - The {W/B}{W/B} ability gating on `Conditions.SourceHasSubtype(CITIZEN)`: it turns Frodo into a
 *    2/3 Halfling Scout with lifelink, and does nothing if he isn't currently a Citizen.
 *  - The {B}{B}{B} ability gating on Scout: it makes the Scout a Halfling Rogue (keeping the 2/3 base
 *    P/T from the Scout step) and permanently grants the combat-damage triggered ability.
 *  - The granted Rogue trigger via the new `Conditions.RingHasTemptedYouAtLeast`: combat damage to a
 *    player makes that player lose the game when the Ring has tempted you 4+ times this game,
 *    otherwise the Ring tempts you.
 */
class FrodoSauronsBaneScenarioTest : FunSpec({

    // Auto-generated ids — read them off the registered definition in declaration order.
    val scoutAbilityId = FrodoSauronsBane.activatedAbilities[0].id
    val rogueAbilityId = FrodoSauronsBane.activatedAbilities[1].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.makeScout(player: EntityId, frodo: EntityId) {
        giveMana(player, Color.BLACK, 2) // {W/B}{W/B} payable with black
        submitSuccess(ActivateAbility(playerId = player, sourceId = frodo, abilityId = scoutAbilityId))
        bothPass()
    }

    fun GameTestDriver.makeRogue(player: EntityId, frodo: EntityId) {
        giveMana(player, Color.BLACK, 3) // {B}{B}{B}
        submitSuccess(ActivateAbility(playerId = player, sourceId = frodo, abilityId = rogueAbilityId))
        bothPass()
    }

    test("{W/B}{W/B} turns the Citizen Frodo into a 2/3 Halfling Scout with lifelink") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val frodo = driver.putCreatureOnBattlefield(player, "Frodo, Sauron's Bane")

        // Starts as a 1/2 Halfling Citizen.
        driver.state.projectedState.hasSubtype(frodo, "Citizen") shouldBe true
        driver.state.projectedState.getPower(frodo) shouldBe 1
        driver.state.projectedState.getToughness(frodo) shouldBe 2

        driver.makeScout(player, frodo)

        val projected = driver.state.projectedState
        projected.hasSubtype(frodo, "Halfling") shouldBe true
        projected.hasSubtype(frodo, "Scout") shouldBe true
        projected.hasSubtype(frodo, "Citizen") shouldBe false
        projected.getPower(frodo) shouldBe 2
        projected.getToughness(frodo) shouldBe 3
        projected.hasKeyword(frodo, Keyword.LIFELINK) shouldBe true
    }

    test("{W/B}{W/B} does nothing once Frodo is no longer a Citizen") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val frodo = driver.putCreatureOnBattlefield(player, "Frodo, Sauron's Bane")

        driver.makeScout(player, frodo) // now a Scout
        driver.makeScout(player, frodo) // gate is false — no change

        val projected = driver.state.projectedState
        projected.hasSubtype(frodo, "Scout") shouldBe true
        projected.getPower(frodo) shouldBe 2
        projected.getToughness(frodo) shouldBe 3
    }

    test("{B}{B}{B} turns the Scout into a Halfling Rogue, keeping its 2/3 base P/T") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val frodo = driver.putCreatureOnBattlefield(player, "Frodo, Sauron's Bane")

        // {B}{B}{B} before being a Scout does nothing — still a Citizen.
        driver.makeRogue(player, frodo)
        driver.state.projectedState.hasSubtype(frodo, "Citizen") shouldBe true
        driver.state.projectedState.hasSubtype(frodo, "Rogue") shouldBe false

        driver.makeScout(player, frodo)
        driver.makeRogue(player, frodo)

        val projected = driver.state.projectedState
        projected.hasSubtype(frodo, "Halfling") shouldBe true
        projected.hasSubtype(frodo, "Rogue") shouldBe true
        projected.hasSubtype(frodo, "Scout") shouldBe false
        // Base P/T unchanged by the Rogue step (no new P/T printed).
        projected.getPower(frodo) shouldBe 2
        projected.getToughness(frodo) shouldBe 3
    }

    test("Rogue's combat damage makes the player lose when the Ring tempted you 4+ times") {
        val driver = createDriver()
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)
        val frodo = driver.putCreatureOnBattlefield(attacker, "Frodo, Sauron's Bane")

        driver.makeScout(attacker, frodo)
        driver.makeRogue(attacker, frodo)
        driver.removeSummoningSickness(frodo)

        // The Ring has tempted you four times this game.
        driver.addComponent(attacker, TheRingComponent(temptCount = 4))

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(frodo), defender).isSuccess shouldBe true
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareNoBlockers(defender)
        // Resolve combat damage and the resulting "loses the game" trigger. The game ends mid-combat
        // once the defender loses, so step through priority/decisions until that happens rather than
        // advancing to a later step (which would never be reached).
        var guard = 0
        while (!driver.state.gameOver && guard++ < 50) {
            if (driver.state.pendingDecision != null) driver.autoResolveDecision()
            else driver.bothPass()
        }

        driver.state.gameOver shouldBe true
    }

    test("Rogue's combat damage tempts you when the Ring has tempted you fewer than 4 times") {
        val driver = createDriver()
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)
        val frodo = driver.putCreatureOnBattlefield(attacker, "Frodo, Sauron's Bane")

        driver.makeScout(attacker, frodo)
        driver.makeRogue(attacker, frodo)
        driver.removeSummoningSickness(frodo)

        // Tempted only three times — below the threshold, so combat damage tempts again.
        driver.addComponent(attacker, TheRingComponent(temptCount = 3))

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(frodo), defender).isSuccess shouldBe true
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareNoBlockers(defender)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        while (driver.state.stack.isNotEmpty() || driver.state.pendingDecision != null) {
            driver.bothPass()
        }

        // Defender still in the game; attacker has been tempted a fourth time.
        driver.state.gameOver shouldBe false
        driver.state.getEntity(attacker)?.get<TheRingComponent>()?.temptCount shouldBe 4
    }
})
