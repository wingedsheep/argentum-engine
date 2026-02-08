package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AnyPlayerMayPayEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.OnEnterBattlefield
import com.wingedsheep.sdk.scripting.PayCost
import com.wingedsheep.sdk.scripting.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.TriggeredAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Prowling Pangolin's "any player may sacrifice two creatures"
 * ETB trigger.
 *
 * Oracle: "When Prowling Pangolin enters the battlefield, any player may
 * sacrifice two creatures. If a player does, sacrifice Prowling Pangolin."
 */
class ProwlingPangolinTest : FunSpec({

    val ProwlingPangolin = CardDefinition(
        name = "Prowling Pangolin",
        manaCost = ManaCost.parse("{3}{B}{B}"),
        typeLine = TypeLine.creature(setOf(Subtype("Pangolin"), Subtype("Beast"))),
        oracleText = "When Prowling Pangolin enters the battlefield, any player may sacrifice two creatures. If a player does, sacrifice Prowling Pangolin.",
        creatureStats = CreatureStats(6, 5),
        script = CardScript.creature(
            TriggeredAbility.create(
                trigger = OnEnterBattlefield(),
                effect = AnyPlayerMayPayEffect(
                    cost = PayCost.Sacrifice(GameObjectFilter.Creature, count = 2),
                    consequence = SacrificeSelfEffect
                )
            )
        )
    )

    // Small creature for sacrificing
    val GoblinToken = CardDefinition(
        name = "Goblin Soldier",
        manaCost = ManaCost.parse("{R}"),
        typeLine = TypeLine.creature(setOf(Subtype("Goblin"))),
        creatureStats = CreatureStats(1, 1),
        script = CardScript.creature()
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ProwlingPangolin)
        driver.registerCard(GoblinToken)
        return driver
    }

    /** Get the opponent of the given player */
    fun GameTestDriver.opponentOf(playerId: com.wingedsheep.sdk.model.EntityId) =
        if (playerId == player1) player2 else player1

    test("Prowling Pangolin stays when no player can sacrifice two creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // No creatures on the battlefield for either player
        val pangolin = driver.putCardInHand(activePlayer, "Prowling Pangolin")
        driver.giveMana(activePlayer, Color.BLACK, 5)

        driver.castSpell(activePlayer, pangolin)
        driver.bothPass() // Resolve creature

        // Resolve ETB trigger - no player can pay, so nothing happens
        if (driver.stackSize > 0) {
            driver.bothPass()
        }

        // No decision needed - nobody can sacrifice 2 creatures
        driver.pendingDecision shouldBe null

        // Pangolin should be on the battlefield
        driver.findPermanent(activePlayer, "Prowling Pangolin") shouldNotBe null
    }

    test("Active player is asked first and can sacrifice to destroy Pangolin") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put two creatures on battlefield for the active player
        val goblin1 = driver.putCreatureOnBattlefield(activePlayer, "Goblin Soldier")
        val goblin2 = driver.putCreatureOnBattlefield(activePlayer, "Goblin Soldier")

        // Cast Prowling Pangolin
        val pangolin = driver.putCardInHand(activePlayer, "Prowling Pangolin")
        driver.giveMana(activePlayer, Color.BLACK, 5)

        driver.castSpell(activePlayer, pangolin)
        driver.bothPass() // Resolve creature

        // Resolve ETB trigger
        driver.stackSize shouldBe 1
        driver.bothPass()

        // Active player should be asked to sacrifice
        val decision = driver.pendingDecision
        decision shouldNotBe null
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        (decision as SelectCardsDecision).playerId shouldBe activePlayer
        decision.maxSelections shouldBe 2

        // Active player sacrifices two goblins
        driver.submitCardSelection(activePlayer, listOf(goblin1, goblin2))

        // Pangolin should be sacrificed
        driver.findPermanent(activePlayer, "Prowling Pangolin") shouldBe null
        driver.getGraveyardCardNames(activePlayer) shouldContain "Prowling Pangolin"

        // Goblins should also be in graveyard
        driver.getGraveyardCardNames(activePlayer).count { it == "Goblin Soldier" } shouldBe 2
    }

    test("Pangolin stays when active player declines and opponent has no creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put two creatures on battlefield for the active player only
        driver.putCreatureOnBattlefield(activePlayer, "Goblin Soldier")
        driver.putCreatureOnBattlefield(activePlayer, "Goblin Soldier")

        // Cast Prowling Pangolin
        val pangolin = driver.putCardInHand(activePlayer, "Prowling Pangolin")
        driver.giveMana(activePlayer, Color.BLACK, 5)

        driver.castSpell(activePlayer, pangolin)
        driver.bothPass() // Resolve creature

        // Resolve ETB trigger
        driver.stackSize shouldBe 1
        driver.bothPass()

        // Active player declines to sacrifice
        driver.submitCardSelection(activePlayer, emptyList())

        // No more decisions - opponent has no creatures
        driver.pendingDecision shouldBe null

        // Pangolin should stay on the battlefield
        driver.findPermanent(activePlayer, "Prowling Pangolin") shouldNotBe null
    }

    test("Opponent gets to sacrifice if active player declines") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.opponentOf(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put two creatures on both players' battlefields
        driver.putCreatureOnBattlefield(activePlayer, "Goblin Soldier")
        driver.putCreatureOnBattlefield(activePlayer, "Goblin Soldier")
        val oppGoblin1 = driver.putCreatureOnBattlefield(opponent, "Goblin Soldier")
        val oppGoblin2 = driver.putCreatureOnBattlefield(opponent, "Goblin Soldier")

        // Cast Prowling Pangolin
        val pangolin = driver.putCardInHand(activePlayer, "Prowling Pangolin")
        driver.giveMana(activePlayer, Color.BLACK, 5)

        driver.castSpell(activePlayer, pangolin)
        driver.bothPass() // Resolve creature

        // Resolve ETB trigger
        driver.stackSize shouldBe 1
        driver.bothPass()

        // Active player declines
        driver.submitCardSelection(activePlayer, emptyList())

        // Opponent should now be asked
        val decision = driver.pendingDecision
        decision shouldNotBe null
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        (decision as SelectCardsDecision).playerId shouldBe opponent

        // Opponent sacrifices two goblins
        driver.submitCardSelection(opponent, listOf(oppGoblin1, oppGoblin2))

        // Pangolin should be sacrificed
        driver.findPermanent(activePlayer, "Prowling Pangolin") shouldBe null
        driver.getGraveyardCardNames(activePlayer) shouldContain "Prowling Pangolin"

        // Opponent's goblins should be in their graveyard
        driver.getGraveyardCardNames(opponent).count { it == "Goblin Soldier" } shouldBe 2
    }

    test("Pangolin stays when both players decline to sacrifice") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.opponentOf(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put two creatures on both players' battlefields
        driver.putCreatureOnBattlefield(activePlayer, "Goblin Soldier")
        driver.putCreatureOnBattlefield(activePlayer, "Goblin Soldier")
        driver.putCreatureOnBattlefield(opponent, "Goblin Soldier")
        driver.putCreatureOnBattlefield(opponent, "Goblin Soldier")

        // Cast Prowling Pangolin
        val pangolin = driver.putCardInHand(activePlayer, "Prowling Pangolin")
        driver.giveMana(activePlayer, Color.BLACK, 5)

        driver.castSpell(activePlayer, pangolin)
        driver.bothPass() // Resolve creature

        // Resolve ETB trigger
        driver.stackSize shouldBe 1
        driver.bothPass()

        // Active player declines
        driver.submitCardSelection(activePlayer, emptyList())

        // Opponent should now be asked
        val opponentDecision = driver.pendingDecision
        opponentDecision shouldNotBe null
        opponentDecision.shouldBeInstanceOf<SelectCardsDecision>()
        (opponentDecision as SelectCardsDecision).playerId shouldBe opponent

        // Opponent also declines
        driver.submitCardSelection(opponent, emptyList())

        // Pangolin should stay on the battlefield
        driver.findPermanent(activePlayer, "Prowling Pangolin") shouldNotBe null
    }
})
