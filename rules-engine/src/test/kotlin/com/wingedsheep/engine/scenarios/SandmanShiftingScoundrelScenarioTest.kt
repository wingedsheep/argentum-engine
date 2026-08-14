package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.SandmanShiftingScoundrel
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sandman, Shifting Scoundrel (SPM #112) — {1}{G}{G} Legendary Creature — Sand Elemental Villain.
 *
 * "Sandman's power and toughness are each equal to the number of lands you control.
 *  Sandman can't be blocked by creatures with power 2 or less.
 *  {3}{G}{G}: Return this card and target land card from your graveyard to the battlefield tapped."
 *
 * Covers all three abilities: the `*`/`*` characteristic-defining ability tracking land count
 * (`dynamicStats(DynamicAmounts.landsYouControl())`), the [com.wingedsheep.sdk.scripting.CantBeBlockedBy]
 * evasion static keyed on blocker power, and the graveyard-zone activated ability that returns
 * Sandman itself plus a targeted land card to the battlefield tapped.
 */
class SandmanShiftingScoundrelScenarioTest : FunSpec({

    val abilityId = SandmanShiftingScoundrel.activatedAbilities.first().id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("power and toughness each equal the number of lands you control") {
        val driver = newDriver()
        val player = driver.player1

        // Three lands you control → Sandman is 3/3.
        repeat(3) { driver.putLandOnBattlefield(player, "Forest") }
        val sandman = driver.putCreatureOnBattlefield(player, "Sandman, Shifting Scoundrel")

        driver.state.projectedState.getPower(sandman) shouldBe 3
        driver.state.projectedState.getToughness(sandman) shouldBe 3

        // A fourth land → 4/4, recomputed continuously (CDA, not a snapshot).
        driver.putLandOnBattlefield(player, "Forest")
        driver.state.projectedState.getPower(sandman) shouldBe 4
        driver.state.projectedState.getToughness(sandman) shouldBe 4
    }

    // Set Sandman up attacking, declare [blockerName] as a blocker, and return the result. Proves
    // the "can't be blocked by creatures with power 2 or less" static is enforced in combat.
    fun declareBlockOnSandman(blockerName: String) = run {
        val driver = newDriver()
        val attacker = driver.player1
        val defender = driver.player2

        // Two lands → Sandman is a 2/2 attacker (alive and able to attack).
        repeat(2) { driver.putLandOnBattlefield(attacker, "Forest") }
        val sandman = driver.putCreatureOnBattlefield(attacker, "Sandman, Shifting Scoundrel")
        driver.removeSummoningSickness(sandman)

        val blocker = driver.putCreatureOnBattlefield(defender, blockerName)
        driver.removeSummoningSickness(blocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(sandman), defender).isSuccess shouldBe true
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(defender, mapOf(blocker to listOf(sandman)))
    }

    test("can't be blocked by a power-2 creature") {
        // Grizzly Bears is 2/2 — power 2, caught by the restriction.
        declareBlockOnSandman("Grizzly Bears").isSuccess shouldBe false
    }

    test("a power-3 creature may still block") {
        // Hill Giant is 3/3 — power 3, unaffected by the restriction.
        declareBlockOnSandman("Hill Giant").isSuccess shouldBe true
    }

    test("{3}{G}{G} graveyard ability returns Sandman and a target land to the battlefield tapped") {
        val driver = newDriver()
        val player = driver.player1

        val sandman = driver.putCardInGraveyard(player, "Sandman, Shifting Scoundrel")
        val landInGrave = driver.putCardInGraveyard(player, "Forest")
        driver.giveMana(player, Color.GREEN, 5) // {3}{G}{G}

        driver.submitSuccess(
            ActivateAbility(
                playerId = player,
                sourceId = sandman,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Card(landInGrave, ownerId = player, zone = Zone.GRAVEYARD)),
            )
        )
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()

        val battlefield = driver.state.getZone(ZoneKey(player, Zone.BATTLEFIELD))
        val graveyard = driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD))

        // Both Sandman and the land moved from the graveyard onto the battlefield.
        battlefield.contains(sandman) shouldBe true
        battlefield.contains(landInGrave) shouldBe true
        graveyard.contains(sandman) shouldBe false
        graveyard.contains(landInGrave) shouldBe false

        // Both entered tapped.
        driver.isTapped(sandman) shouldBe true
        driver.isTapped(landInGrave) shouldBe true

        // Sandman is back as a live creature; it now controls a land, so its CDA reads at least 1/1.
        driver.state.projectedState.getPower(sandman) shouldNotBe null
    }
})
