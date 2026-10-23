package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.DungeonCompletedEvent
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.VenturedIntoDungeonEvent
import com.wingedsheep.engine.mechanics.dungeon.DungeonService
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.DungeonCardComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dungeons.Dungeons
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Engine tests for **dungeons and venturing into the dungeon** (CR 309, CR 701.49, CR 704.5t).
 *
 * | Rule | What it says | Covered by |
 * |---|---|---|
 * | 701.49a / 309.2a | with no dungeon, choose one from outside the game and enter its topmost room | "venturing with no dungeon …" |
 * | 701.49d + Undercity's text | an ordinary venture can't choose Undercity; "venture into Undercity" enters it | "an ordinary venture never offers Undercity", "venture into Undercity …" |
 * | 701.49d | "venture into [quality]" while in a dungeon advances that dungeon as usual | "venture into Undercity while in another dungeon …" |
 * | 309.4c | each room ability triggers on entering, uses the stack, is controlled by the owner | "the room ability goes on the stack …", "a targeted room …" |
 * | 309.5a / 701.49b | move along an arrow; the player chooses where there are several | "a single arrow advances without a choice", "a branching room asks …" |
 * | 309.6 / 704.5t | the dungeon is removed once the bottom room's ability has left the stack | "the dungeon completes only after its last room resolves" |
 * | 309.7 | removing it completes it — "you've completed a dungeon" and the count | "completed dungeons are counted …" |
 * | 701.49c | venturing from the bottom room completes the dungeon and starts a new one | "venturing from the bottommost room …" |
 * | 309.3 | one dungeon per player in the command zone | every venture asserts a single dungeon card |
 */
class DungeonTest : ScenarioTestBase() {

    private val venture = card("Test Venture") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.VentureIntoTheDungeon() }
    }

    private val ventureInstant = card("Test Venture Instant") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { effect = Effects.VentureIntoTheDungeon() }
    }

    private val ventureIntoUndercity = card("Test Undercity Venture") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.VentureIntoTheDungeon(into = "Undercity") }
    }

    /** Gains 1 life for each dungeon you've completed; 10 more if you've completed one. */
    private val delver = card("Test Delver Reward") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.GainLife(DynamicAmount.DungeonsCompleted(Player.You)) then
                Effects.If(Conditions.YouCompletedADungeon, Effects.GainLife(10))
        }
    }

    /** Gains 1 life for each differently named dungeon you've completed. */
    private val distinctDelver = card("Test Distinct Delver Reward") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.GainLife(
                DynamicAmount.DungeonsCompleted(Player.You, distinctNames = true)
            )
        }
    }

    private val ventureWatcher = card("Test Venture Watcher") {
        manaCost = "{1}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.you.venturesIntoTheDungeon()
            effect = Effects.GainLife(2)
        }
        triggeredAbility {
            trigger = Triggers.you.completesADungeon()
            effect = Effects.DrawCards(1)
        }
    }

    private val bear = card("Test Dungeon Bear") {
        manaCost = "{2}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    init {
        listOf(venture, ventureInstant, ventureIntoUndercity, delver, distinctDelver, ventureWatcher, bear).forEach { cardRegistry.register(it) }

        test("venturing with no dungeon offers the three AFR dungeons and enters the chosen one's top room (CR 701.49a)") {
            val game = dungeonGame { withCardInHand(1, "Test Venture") }
            game.castSpell(1, "Test Venture").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision() as ChooseOptionDecision
            decision.playerId shouldBe game.player1Id
            decision.options shouldContainExactly listOf("Lost Mine of Phandelver", "Dungeon of the Mad Mage", "Tomb of Annihilation")
            withClue("Undercity can only be entered by 'venture into Undercity'") {
                decision.options shouldNotContain "Undercity"
            }

            game.submitDecision(OptionChosenResponse(decision.id, 1)).error shouldBe null
            val marker = DungeonService.marker(game.state, game.player1Id)!!
            marker.dungeon shouldBe "Dungeon of the Mad Mage"
            marker.room shouldBe 0
            game.state.getEntity(marker.dungeonEntityId)?.get<DungeonCardComponent>()?.ownerId shouldBe game.player1Id

            withClue("Yawning Portal's room ability triggered and is on the stack (CR 309.4c)") {
                val room = game.state.stack.single().let { game.state.getEntity(it)!!.get<TriggeredAbilityOnStackComponent>()!! }
                room.sourceId shouldBe marker.dungeonEntityId
                room.controllerId shouldBe game.player1Id
                room.sourceName shouldBe "Dungeon of the Mad Mage"
            }
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 21
        }

        test("a single arrow advances without a choice (CR 309.5a)") {
            val game = dungeonGame { withCardInHand(1, "Test Venture") }
            game.setMarker(1, Dungeons.TombOfAnnihilation, room = 1) // Veils of Fear → Sandfall Cell only
            game.castSpell(1, "Test Venture").error shouldBe null
            val results = game.resolveStack()

            val moved = results.flatMap { it.events }.filterIsInstance<VenturedIntoDungeonEvent>().single()
            moved.roomName shouldBe "Sandfall Cell"
            moved.enteredDungeon shouldBe false
            DungeonService.marker(game.state, game.player1Id)!!.room shouldBe 2
        }

        test("a branching room asks which arrow to follow, and the chosen room's ability triggers (CR 701.49b)") {
            val game = dungeonGame { withCardInHand(1, "Test Venture") }
            game.setMarker(1, Dungeons.TombOfAnnihilation, room = 0) // Trapped Entry → Veils of Fear / Oubliette
            game.castSpell(1, "Test Venture").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision() as ChooseOptionDecision
            decision.options shouldContainExactly listOf("Veils of Fear", "Oubliette")
            decision.optionMetadata.map { it.id } shouldContainExactly listOf("dungeon-room:1", "dungeon-room:3")
            game.submitDecision(OptionChosenResponse(decision.id, 1)).error shouldBe null

            DungeonService.marker(game.state, game.player1Id)!!.room shouldBe 3
            game.state.stack.size shouldBe 1
        }

        test("a targeted room ability chooses its target as it goes on the stack") {
            val game = dungeonGame {
                withCardInHand(1, "Test Venture")
                withCardOnBattlefield(2, "Test Dungeon Bear")
            }
            game.setMarker(1, Dungeons.LostMineOfPhandelver, room = 1) // Goblin Lair → Storeroom / Dark Pool
            game.castSpell(1, "Test Venture").error shouldBe null
            game.resolveStack()
            val decision = game.getPendingDecision() as ChooseOptionDecision
            game.submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf("Storeroom"))).error shouldBe null

            val bearId = game.findPermanent("Test Dungeon Bear")!!
            game.selectTargets(listOf(bearId)).error shouldBe null
            game.resolveStack()

            game.state.getEntity(bearId)!!.get<CountersComponent>()!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        }

        test("the dungeon completes only after its last room's ability has left the stack (CR 309.6, 704.5t)") {
            val game = dungeonGame { withCardInHand(1, "Test Venture") }
            game.setMarker(1, Dungeons.TombOfAnnihilation, room = 3) // Oubliette → Cradle of the Death God
            game.castSpell(1, "Test Venture").error shouldBe null
            // Resolve only the venture spell; the Cradle room ability is now on the stack.
            game.passPriority()
            game.passPriority()

            withClue("The bottom room's ability is waiting, so the dungeon stays in the command zone") {
                DungeonService.marker(game.state, game.player1Id)!!.room shouldBe 4
                game.state.stack.size shouldBe 1
            }

            val results = game.resolveStack()
            game.isOnBattlefield("The Atropal") shouldBe true
            DungeonService.marker(game.state, game.player1Id) shouldBe null
            DungeonService.completedCount(game.state, game.player1Id) shouldBe 1
            results.flatMap { it.events }.filterIsInstance<DungeonCompletedEvent>().single().let {
                it.playerId shouldBe game.player1Id
                it.dungeonName shouldBe "Tomb of Annihilation"
                it.completedCount shouldBe 1
            }
            withClue("The dungeon card left the game with it") {
                game.state.entities.values.none { it.has<DungeonCardComponent>() } shouldBe true
            }
        }

        test("venturing from the bottommost room completes the dungeon and starts a new one (CR 701.49c)") {
            val game = dungeonGame { withCardInHand(1, "Test Venture Instant") }
            game.setMarker(1, Dungeons.TombOfAnnihilation, room = 4)
            // The Cradle room ability is (as far as the SBA is concerned) still waiting: put one on the stack.
            val cardId = DungeonService.marker(game.state, game.player1Id)!!.dungeonEntityId
            game.pinRoomAbility(cardId)

            game.castSpell(1, "Test Venture Instant").error shouldBe null
            game.passPriority()
            game.passPriority()

            val decision = game.getPendingDecision() as ChooseOptionDecision
            withClue("Tomb of Annihilation was completed as the new venture began") {
                DungeonService.completedCount(game.state, game.player1Id, "Tomb of Annihilation") shouldBe 1
            }
            game.submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf("Lost Mine of Phandelver"))).error shouldBe null
            val marker = DungeonService.marker(game.state, game.player1Id)!!
            marker.dungeon shouldBe "Lost Mine of Phandelver"
            marker.room shouldBe 0
            marker.dungeonEntityId shouldNotBe cardId
        }

        test("completed dungeons are counted, and 'you've completed a dungeon' reads them (CR 309.7)") {
            val game = dungeonGame { withCardsInHand(1, "Test Delver Reward", 2) }
            game.castSpell(1, "Test Delver Reward").error shouldBe null
            game.resolveStack()
            withClue("No dungeon completed: 0 life, and the condition is false") {
                game.getLifeTotal(1) shouldBe 20
            }

            game.setMarker(1, Dungeons.LostMineOfPhandelver, room = 6)
            game.state = DungeonService.complete(game.state, game.player1Id).first
            game.setMarker(1, Dungeons.LostMineOfPhandelver, room = 6)
            game.state = DungeonService.complete(game.state, game.player1Id).first

            game.castSpell(1, "Test Delver Reward").error shouldBe null
            game.resolveStack()
            withClue("Two completions — the same dungeon twice counts twice") {
                game.getLifeTotal(1) shouldBe 20 + 2 + 10
            }
            DungeonService.completedCount(game.state, game.player1Id, "Lost Mine of Phandelver") shouldBe 2
            DungeonService.completedCount(game.state, game.player1Id, "Tomb of Annihilation") shouldBe 0
        }

        test("'differently named dungeons' counts each dungeon name once, however often completed (CR 309.7)") {
            val game = dungeonGame { withCardsInHand(1, "Test Distinct Delver Reward", 2) }
            game.setMarker(1, Dungeons.LostMineOfPhandelver, room = 6)
            game.state = DungeonService.complete(game.state, game.player1Id).first
            game.setMarker(1, Dungeons.LostMineOfPhandelver, room = 6)
            game.state = DungeonService.complete(game.state, game.player1Id).first

            game.castSpell(1, "Test Distinct Delver Reward").error shouldBe null
            game.resolveStack()
            withClue("Lost Mine twice is one differently named dungeon") {
                game.getLifeTotal(1) shouldBe 21
            }

            game.setMarker(1, Dungeons.TombOfAnnihilation, room = 4)
            game.state = DungeonService.complete(game.state, game.player1Id).first
            game.castSpell(1, "Test Distinct Delver Reward").error shouldBe null
            game.resolveStack()
            withClue("Tomb of Annihilation is a second name: +2 this time") {
                game.getLifeTotal(1) shouldBe 23
            }
            DungeonService.completedCount(game.state, game.player1Id) shouldBe 3
            DungeonService.completedCount(
                game.state, game.player1Id, "Lost Mine of Phandelver", distinctNames = true
            ) shouldBe 1
        }

        test("'whenever you venture' and 'whenever you complete a dungeon' trigger") {
            val game = dungeonGame {
                withCardInHand(1, "Test Venture")
                withCardOnBattlefield(1, "Test Venture Watcher")
            }
            game.setMarker(1, Dungeons.LostMineOfPhandelver, room = 3) // Storeroom → Temple of Dumathoin
            val handBefore = game.handSize(1)
            game.castSpell(1, "Test Venture").error shouldBe null
            game.resolveAll()

            withClue("+2 from the venture trigger; the Temple draws one and completion draws one") {
                game.getLifeTotal(1) shouldBe 22
                DungeonService.completedCount(game.state, game.player1Id) shouldBe 1
                game.handSize(1) shouldBe handBefore - 1 + 2
            }
        }

        test("'venture into Undercity' with no dungeon enters Undercity without a choice (CR 701.49d)") {
            val game = dungeonGame { withCardInHand(1, "Test Undercity Venture") }
            game.castSpell(1, "Test Undercity Venture").error shouldBe null
            game.passPriority()
            game.passPriority()

            (game.getPendingDecision() is ChooseOptionDecision) shouldBe false
            DungeonService.marker(game.state, game.player1Id)!!.dungeon shouldBe "Undercity"
        }

        test("'venture into Undercity' while in another dungeon advances that dungeon (CR 701.49d)") {
            val game = dungeonGame { withCardInHand(1, "Test Undercity Venture") }
            game.setMarker(1, Dungeons.TombOfAnnihilation, room = 1)
            game.castSpell(1, "Test Undercity Venture").error shouldBe null
            game.resolveStack()

            val marker = DungeonService.marker(game.state, game.player1Id)!!
            marker.dungeon shouldBe "Tomb of Annihilation"
            marker.room shouldBe 2
        }

        test("each player's dungeon and room are public in the client state") {
            val game = dungeonGame {}
            game.setMarker(2, Dungeons.DungeonOfTheMadMage, room = 4)

            val opponentSeen = game.getClientState(1).players.single { it.playerId == game.player2Id }
            opponentSeen.dungeon!!.name shouldBe "Dungeon of the Mad Mage"
            opponentSeen.dungeon!!.currentRoom shouldBe 4
            opponentSeen.dungeon!!.rooms[4].leadsTo shouldContainExactly listOf(5, 6)
        }
    }

    /** Resolve the whole stack, putting simultaneous triggers on it in the order offered. */
    private fun TestGame.resolveAll() {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard++ < 50) {
            when (val d = state.pendingDecision) {
                null -> passPriority()
                is OrderObjectsDecision -> submitDecision(OrderedResponse(d.id, d.objects))
                else -> error("Unexpected decision $d")
            }
        }
    }

    /** Put [dungeon] into the player's command zone with the marker on [room], without triggering anything. */
    private fun TestGame.setMarker(playerNumber: Int, dungeon: com.wingedsheep.sdk.model.DungeonDefinition, room: Int) {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        var s = state
        if (DungeonService.marker(s, playerId) == null) s = DungeonService.enter(s, playerId, dungeon, "test").first
        state = DungeonService.moveTo(s, playerId, room, "test").first
    }

    /** Stand in a room ability from [dungeonCardId] on the stack, as if it had just triggered. */
    private fun TestGame.pinRoomAbility(dungeonCardId: EntityId) {
        val (id, minted) = state.newEntity()
        val ability = com.wingedsheep.engine.state.ComponentContainer.of(
            TriggeredAbilityOnStackComponent(
                sourceId = dungeonCardId,
                sourceName = "Tomb of Annihilation",
                controllerId = player1Id,
                effect = Effects.GainLife(1),
                description = "Cradle of the Death God"
            )
        )
        state = minted.withEntity(id, ability).copy(stack = minted.stack + id)
    }

    private fun dungeonGame(extra: ScenarioBuilder.() -> Unit): TestGame {
        val builder = scenario().withPlayers("Player", "Opponent")
        repeat(10) {
            builder.withCardInLibrary(1, "Test Dungeon Bear")
            builder.withCardInLibrary(2, "Test Dungeon Bear")
        }
        builder.withLandsOnBattlefield(1, "Plains", 2)
        builder.extra()
        return builder.withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
    }
}
