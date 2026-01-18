package com.wingedsheep.rulesengine.action

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.game.GameState
import com.wingedsheep.rulesengine.game.Step
import com.wingedsheep.rulesengine.player.Player
import com.wingedsheep.rulesengine.player.PlayerId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class CastingActionsTest : FunSpec({

    val player1Id = PlayerId.of("player1")
    val player2Id = PlayerId.of("player2")

    fun createPlayer1() = Player.create(player1Id, "Alice")
    fun createPlayer2() = Player.create(player2Id, "Bob")

    val bearDef = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAST),
        power = 2,
        toughness = 2
    )

    val lightningBoltDef = CardDefinition.instant(
        name = "Lightning Bolt",
        manaCost = ManaCost.parse("{R}"),
        oracleText = "Deal 3 damage to any target."
    )

    val lavaAxeDef = CardDefinition.sorcery(
        name = "Lava Axe",
        manaCost = ManaCost.parse("{4}{R}"),
        oracleText = "Deal 5 damage to target player."
    )

    fun createGameInMainPhase(): GameState {
        val state = GameState.newGame(createPlayer1(), createPlayer2())
        return state.advanceToStep(Step.PRECOMBAT_MAIN)
    }

    context("CastSpell action") {
        test("casts creature spell from hand to stack") {
            var state = createGameInMainPhase()
            val bear = CardInstance.create(bearDef, player1Id.value)
            state = state.updatePlayer(player1Id) { p ->
                p.updateHand { it.addToTop(bear) }
                    .addMana(Color.GREEN, 1)
                    .addColorlessMana(1)
            }

            val action = CastSpell(bear.id, player1Id)
            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.stack.contains(bear.id).shouldBeTrue()
            result.state.getPlayer(player1Id).hand.contains(bear.id).shouldBeFalse()
        }

        test("casts instant spell") {
            var state = createGameInMainPhase()
            val bolt = CardInstance.create(lightningBoltDef, player1Id.value)
            state = state.updatePlayer(player1Id) { p ->
                p.updateHand { it.addToTop(bolt) }
                    .addMana(Color.RED, 1)
            }

            val action = CastSpell(bolt.id, player1Id)
            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.stack.contains(bolt.id).shouldBeTrue()
        }

        test("pays mana cost when casting") {
            var state = createGameInMainPhase()
            val bear = CardInstance.create(bearDef, player1Id.value)
            state = state.updatePlayer(player1Id) { p ->
                p.updateHand { it.addToTop(bear) }
                    .addMana(Color.GREEN, 2)
                    .addColorlessMana(2)
            }

            val action = CastSpell(bear.id, player1Id)
            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            // Should have spent 1G + 1 generic
            result.state.getPlayer(player1Id).manaPool.get(Color.GREEN) shouldBe 1
            result.state.getPlayer(player1Id).manaPool.colorless shouldBe 1
        }

        test("fails when card is not in hand") {
            val state = createGameInMainPhase()
            val bear = CardInstance.create(bearDef, player1Id.value)

            val action = CastSpell(bear.id, player1Id)
            val result = ActionExecutor.execute(state, action)

            result.isFailure.shouldBeTrue()
            (result as ActionResult.Failure).reason shouldBe "Card not in hand"
        }

        test("fails when not enough mana") {
            var state = createGameInMainPhase()
            val bear = CardInstance.create(bearDef, player1Id.value)
            state = state.updatePlayer(player1Id) { p ->
                p.updateHand { it.addToTop(bear) }
            }

            val action = CastSpell(bear.id, player1Id)
            val result = ActionExecutor.execute(state, action)

            result.isFailure.shouldBeTrue()
        }

        test("fails when timing is wrong for sorcery") {
            var state = createGameInMainPhase()
                .advanceToStep(Step.DECLARE_ATTACKERS)
            val sorcery = CardInstance.create(lavaAxeDef, player1Id.value)
            state = state.updatePlayer(player1Id) { p ->
                p.updateHand { it.addToTop(sorcery) }
                    .addMana(Color.RED, 1)
                    .addColorlessMana(4)
            }

            val action = CastSpell(sorcery.id, player1Id)
            val result = ActionExecutor.execute(state, action)

            result.isFailure.shouldBeTrue()
            (result as ActionResult.Failure).reason shouldBe
                "Sorcery-speed spells can only be cast during main phase"
        }

        test("generates CardMoved event") {
            var state = createGameInMainPhase()
            val bolt = CardInstance.create(lightningBoltDef, player1Id.value)
            state = state.updatePlayer(player1Id) { p ->
                p.updateHand { it.addToTop(bolt) }
                    .addMana(Color.RED, 1)
            }

            val action = CastSpell(bolt.id, player1Id)
            val result = ActionExecutor.execute(state, action) as ActionResult.Success

            result.events.any { it is GameEvent.CardMoved } shouldBe true
            val event = result.events.filterIsInstance<GameEvent.CardMoved>().first()
            event.fromZone shouldBe "HAND"
            event.toZone shouldBe "STACK"
        }
    }

    context("PayManaCost action") {
        test("pays colored mana") {
            var state = createGameInMainPhase()
            state = state.updatePlayer(player1Id) { p ->
                p.addMana(Color.RED, 2).addMana(Color.GREEN, 1)
            }

            val action = PayManaCost(player1Id, red = 1, green = 1)
            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.getPlayer(player1Id).manaPool.get(Color.RED) shouldBe 1
            result.state.getPlayer(player1Id).manaPool.get(Color.GREEN) shouldBe 0
        }

        test("pays generic mana") {
            var state = createGameInMainPhase()
            state = state.updatePlayer(player1Id) { p ->
                p.addMana(Color.RED, 3)
            }

            val action = PayManaCost(player1Id, generic = 2)
            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.getPlayer(player1Id).manaPool.get(Color.RED) shouldBe 1
        }

        test("fails when not enough mana") {
            val state = createGameInMainPhase()

            val action = PayManaCost(player1Id, red = 1)
            val result = ActionExecutor.execute(state, action)

            result.isFailure.shouldBeTrue()
        }
    }

    context("ResolveTopOfStack action") {
        test("resolves permanent spell to battlefield") {
            var state = createGameInMainPhase()
            val bear = CardInstance.create(bearDef, player1Id.value)
            state = state.updateStack { it.addToTop(bear) }

            val action = ResolveTopOfStack()
            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.stack.isEmpty.shouldBeTrue()
            result.state.battlefield.contains(bear.id).shouldBeTrue()
        }

        test("creature gets summoning sickness on resolution") {
            var state = createGameInMainPhase()
            val bear = CardInstance.create(bearDef, player1Id.value)
            state = state.updateStack { it.addToTop(bear) }

            val action = ResolveTopOfStack()
            val result = ActionExecutor.execute(state, action)

            result.state.battlefield.getCard(bear.id)!!.summoningSickness.shouldBeTrue()
        }

        test("resolves sorcery to graveyard") {
            var state = createGameInMainPhase()
            val sorcery = CardInstance.create(lavaAxeDef, player1Id.value)
            state = state.updateStack { it.addToTop(sorcery) }

            val action = ResolveTopOfStack()
            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.stack.isEmpty.shouldBeTrue()
            result.state.getPlayer(player1Id).graveyard.contains(sorcery.id).shouldBeTrue()
        }

        test("resolves instant to graveyard") {
            var state = createGameInMainPhase()
            val instant = CardInstance.create(lightningBoltDef, player1Id.value)
            state = state.updateStack { it.addToTop(instant) }

            val action = ResolveTopOfStack()
            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.getPlayer(player1Id).graveyard.contains(instant.id).shouldBeTrue()
        }

        test("fails when stack is empty") {
            val state = createGameInMainPhase()

            val action = ResolveTopOfStack()
            val result = ActionExecutor.execute(state, action)

            result.isFailure.shouldBeTrue()
            (result as ActionResult.Failure).reason shouldBe "Stack is empty"
        }

        test("resolves in LIFO order") {
            var state = createGameInMainPhase()
            val bolt1 = CardInstance.create(lightningBoltDef, player1Id.value)
            val bolt2 = CardInstance.create(lightningBoltDef, player1Id.value)
            state = state.updateStack { it.addToTop(bolt1).addToTop(bolt2) }

            // First resolution should resolve bolt2 (last in)
            val result1 = ActionExecutor.execute(state, ResolveTopOfStack())
            result1.state.getPlayer(player1Id).graveyard.contains(bolt2.id).shouldBeTrue()
            result1.state.stack.contains(bolt1.id).shouldBeTrue()

            // Second resolution should resolve bolt1
            val result2 = ActionExecutor.execute(result1.state, ResolveTopOfStack())
            result2.state.getPlayer(player1Id).graveyard.contains(bolt1.id).shouldBeTrue()
            result2.state.stack.isEmpty.shouldBeTrue()
        }

        test("resets priority to active player after resolution") {
            var state = createGameInMainPhase()
            val instant = CardInstance.create(lightningBoltDef, player1Id.value)
            state = state
                .updateStack { it.addToTop(instant) }
                .updateTurnState { it.copy(priorityPlayer = player2Id) }

            val action = ResolveTopOfStack()
            val result = ActionExecutor.execute(state, action)

            result.state.turnState.priorityPlayer shouldBe player1Id
        }

        test("generates CardMoved event") {
            var state = createGameInMainPhase()
            val bear = CardInstance.create(bearDef, player1Id.value)
            state = state.updateStack { it.addToTop(bear) }

            val action = ResolveTopOfStack()
            val result = ActionExecutor.execute(state, action) as ActionResult.Success

            result.events.any { it is GameEvent.CardMoved } shouldBe true
        }
    }

    context("PassPriority action") {
        test("passes priority to next player") {
            val state = createGameInMainPhase()

            val action = PassPriority(player1Id)
            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.turnState.priorityPlayer shouldBe player2Id
        }

        test("priority cycles back to first player") {
            val state = createGameInMainPhase()
                .updateTurnState { it.copy(priorityPlayer = player2Id) }

            val action = PassPriority(player2Id)
            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.turnState.priorityPlayer shouldBe player1Id
        }

        test("fails when player does not have priority") {
            val state = createGameInMainPhase()

            val action = PassPriority(player2Id)
            val result = ActionExecutor.execute(state, action)

            result.isFailure.shouldBeTrue()
            (result as ActionResult.Failure).reason shouldBe "player2 does not have priority"
        }
    }

    context("CounterSpell action") {
        test("moves spell from stack to graveyard") {
            var state = createGameInMainPhase()
            val bolt = CardInstance.create(lightningBoltDef, player1Id.value)
            state = state.updateStack { it.addToTop(bolt) }

            val action = CounterSpell(bolt.id)
            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.stack.contains(bolt.id).shouldBeFalse()
            result.state.getPlayer(player1Id).graveyard.contains(bolt.id).shouldBeTrue()
        }

        test("fails when card is not on stack") {
            val state = createGameInMainPhase()
            val bolt = CardInstance.create(lightningBoltDef, player1Id.value)

            val action = CounterSpell(bolt.id)
            val result = ActionExecutor.execute(state, action)

            result.isFailure.shouldBeTrue()
            (result as ActionResult.Failure).reason shouldBe "Card not on stack"
        }

        test("generates CardMoved event") {
            var state = createGameInMainPhase()
            val bolt = CardInstance.create(lightningBoltDef, player1Id.value)
            state = state.updateStack { it.addToTop(bolt) }

            val action = CounterSpell(bolt.id)
            val result = ActionExecutor.execute(state, action) as ActionResult.Success

            result.events.any { it is GameEvent.CardMoved } shouldBe true
            val event = result.events.filterIsInstance<GameEvent.CardMoved>().first()
            event.fromZone shouldBe "STACK"
            event.toZone shouldBe "GRAVEYARD"
        }
    }

    context("full casting flow") {
        test("cast creature, resolve, creature enters battlefield") {
            var state = createGameInMainPhase()
            val bear = CardInstance.create(bearDef, player1Id.value)
            state = state.updatePlayer(player1Id) { p ->
                p.updateHand { it.addToTop(bear) }
                    .addMana(Color.GREEN, 1)
                    .addColorlessMana(1)
            }

            // Cast the spell
            val castResult = ActionExecutor.execute(state, CastSpell(bear.id, player1Id))
            castResult.isSuccess.shouldBeTrue()

            // Both players pass priority (spell resolves)
            val pass1 = ActionExecutor.execute(castResult.state, PassPriority(player1Id))
            val pass2 = ActionExecutor.execute(pass1.state, PassPriority(player2Id))

            // Resolve the spell
            val resolveResult = ActionExecutor.execute(pass2.state, ResolveTopOfStack())

            resolveResult.isSuccess.shouldBeTrue()
            resolveResult.state.battlefield.contains(bear.id).shouldBeTrue()
            resolveResult.state.stack.isEmpty.shouldBeTrue()
        }

        test("cast instant in response to sorcery") {
            var state = createGameInMainPhase()
            val sorcery = CardInstance.create(lavaAxeDef, player1Id.value)
            val instant = CardInstance.create(lightningBoltDef, player2Id.value)

            state = state
                .updatePlayer(player1Id) { p ->
                    p.updateHand { it.addToTop(sorcery) }
                        .addMana(Color.RED, 1)
                        .addColorlessMana(4)
                }
                .updatePlayer(player2Id) { p ->
                    p.updateHand { it.addToTop(instant) }
                        .addMana(Color.RED, 1)
                }

            // Player 1 casts sorcery
            val castSorcery = ActionExecutor.execute(state, CastSpell(sorcery.id, player1Id))

            // Player 1 passes priority
            val pass1 = ActionExecutor.execute(castSorcery.state, PassPriority(player1Id))

            // Player 2 casts instant in response
            val castInstant = ActionExecutor.execute(pass1.state, CastSpell(instant.id, player2Id))

            // Stack should have instant on top of sorcery
            castInstant.state.stack.size shouldBe 2
            castInstant.state.stack.cards.last().id shouldBe instant.id
            castInstant.state.stack.cards.first().id shouldBe sorcery.id
        }
    }
})
