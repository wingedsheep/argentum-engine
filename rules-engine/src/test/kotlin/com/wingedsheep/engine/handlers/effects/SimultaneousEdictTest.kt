package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "Each player sacrifices a creature" (CR 101.4): each player chooses in APNAP order — the active
 * player first, whoever controls the edict — and nothing is sacrificed until the last player has
 * chosen; then every chosen permanent is sacrificed at once.
 */
class SimultaneousEdictTest : FunSpec({

    val bear = card("Edict Test Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    val edict = card("Edict Test Mass Edict") {
        manaCost = "{B}"
        typeLine = "Instant"
        spell {
            effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.PlayerRef(Player.Each))
        }
    }

    test("choices go active player first, then in turn order, and the sacrifices happen together") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(bear, edict))
        // Player 2 starts, so APNAP order is 2, 3, 1.
        val players = driver.initMultiplayer(decks = List(3) { Deck.of("Forest" to 40) }, startingPlayer = 1)
        val (p1, p2, p3) = players
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = p2)

        val bears: Map<EntityId, List<EntityId>> = players.associateWith { p ->
            List(2) { driver.putCreatureOnBattlefield(p, "Edict Test Bear") }
        }
        val spell = driver.putCardInHand(p2, "Edict Test Mass Edict")
        driver.giveMana(p2, Color.BLACK)
        driver.castSpell(p2, spell).error.shouldBeNull()
        repeat(3) {
            if (driver.state.pendingDecision == null) driver.passPriority(driver.state.priorityPlayerId!!)
        }

        for (chooser in listOf(p2, p3, p1)) {
            val decision = driver.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            decision.playerId shouldBe chooser
            // Earlier choosers' creatures are all still on the battlefield.
            players.forEach { p -> driver.getCreatures(p).size shouldBe 2 }
            driver.submitCardSelection(chooser, listOf(bears.getValue(chooser).first())).error.shouldBeNull()
        }

        driver.state.pendingDecision.shouldBeNull()
        players.forEach { p ->
            driver.getCreatures(p) shouldContainExactlyInAnyOrder listOf(bears.getValue(p).last())
            driver.getGraveyardCardNames(p).count { it == "Edict Test Bear" } shouldBe 1
        }
    }
})
