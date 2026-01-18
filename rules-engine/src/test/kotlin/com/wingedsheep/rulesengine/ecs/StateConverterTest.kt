package com.wingedsheep.rulesengine.ecs

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.card.CounterType
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.components.*
import com.wingedsheep.rulesengine.game.GameState
import com.wingedsheep.rulesengine.player.Player
import com.wingedsheep.rulesengine.player.PlayerId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

class StateConverterTest : FunSpec({

    val bearDef = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAST),
        power = 2,
        toughness = 2
    )

    val forestDef = CardDefinition.basicLand("Forest", Subtype.FOREST)

    fun createTwoPlayerGame(): GameState {
        val player1 = Player.create(PlayerId.of("player1"), "Alice")
        val player2 = Player.create(PlayerId.of("player2"), "Bob")
        return GameState.newGame(player1, player2)
    }

    context("GameState to EcsGameState") {
        test("converts players") {
            val gameState = createTwoPlayerGame()

            val ecsState = StateConverter.toEcs(gameState)

            val players = ecsState.getPlayerIds()
            players.size shouldBe 2

            // Check player components
            val player1Id = EntityId.of("player1")
            val playerComponent = ecsState.getComponent<PlayerComponent>(player1Id)
            playerComponent?.name shouldBe "Alice"

            val lifeComponent = ecsState.getComponent<LifeComponent>(player1Id)
            lifeComponent?.life shouldBe 20
        }

        test("converts cards on battlefield") {
            val player1 = Player.create(PlayerId.of("player1"), "Alice")
            val player2 = Player.create(PlayerId.of("player2"), "Bob")
            val bear = CardInstance.create(bearDef, player1.id.value)

            val gameState = GameState.newGame(player1, player2)
                .updateBattlefield { it.addToTop(bear) }

            val ecsState = StateConverter.toEcs(gameState)

            val battlefield = ecsState.getBattlefield()
            battlefield.size shouldBe 1

            val bearId = EntityId.fromCardId(bear.id)
            val cardComponent = ecsState.getComponent<CardComponent>(bearId)
            cardComponent?.name shouldBe "Grizzly Bears"

            val controller = ecsState.getComponent<ControllerComponent>(bearId)
            controller?.controllerId shouldBe EntityId.of("player1")
        }

        test("converts tapped state") {
            val player1 = Player.create(PlayerId.of("player1"), "Alice")
            val player2 = Player.create(PlayerId.of("player2"), "Bob")
            val bear = CardInstance.create(bearDef, player1.id.value).tap()

            val gameState = GameState.newGame(player1, player2)
                .updateBattlefield { it.addToTop(bear) }

            val ecsState = StateConverter.toEcs(gameState)

            val bearId = EntityId.fromCardId(bear.id)
            ecsState.hasComponent<TappedComponent>(bearId).shouldBeTrue()
        }

        test("converts counters") {
            val player1 = Player.create(PlayerId.of("player1"), "Alice")
            val player2 = Player.create(PlayerId.of("player2"), "Bob")
            val bear = CardInstance.create(bearDef, player1.id.value)
                .addCounter(CounterType.PLUS_ONE_PLUS_ONE, 2)

            val gameState = GameState.newGame(player1, player2)
                .updateBattlefield { it.addToTop(bear) }

            val ecsState = StateConverter.toEcs(gameState)

            val bearId = EntityId.fromCardId(bear.id)
            val counters = ecsState.getComponent<CountersComponent>(bearId)
            counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        }

        test("converts damage") {
            val player1 = Player.create(PlayerId.of("player1"), "Alice")
            val player2 = Player.create(PlayerId.of("player2"), "Bob")
            val bear = CardInstance.create(bearDef, player1.id.value)
                .dealDamage(1)

            val gameState = GameState.newGame(player1, player2)
                .updateBattlefield { it.addToTop(bear) }

            val ecsState = StateConverter.toEcs(gameState)

            val bearId = EntityId.fromCardId(bear.id)
            val damage = ecsState.getComponent<DamageComponent>(bearId)
            damage?.amount shouldBe 1
        }

        test("converts keyword modifications") {
            val player1 = Player.create(PlayerId.of("player1"), "Alice")
            val player2 = Player.create(PlayerId.of("player2"), "Bob")
            val bear = CardInstance.create(bearDef, player1.id.value)
                .addKeyword(Keyword.FLYING)
                .addTemporaryKeyword(Keyword.HASTE)

            val gameState = GameState.newGame(player1, player2)
                .updateBattlefield { it.addToTop(bear) }

            val ecsState = StateConverter.toEcs(gameState)

            val bearId = EntityId.fromCardId(bear.id)
            val keywords = ecsState.getComponent<KeywordsComponent>(bearId)
            keywords?.added?.contains(Keyword.FLYING)?.shouldBeTrue()
            keywords?.temporary?.contains(Keyword.HASTE)?.shouldBeTrue()
        }

        test("converts cards in player zones") {
            val player1 = Player.create(PlayerId.of("player1"), "Alice")
            val player2 = Player.create(PlayerId.of("player2"), "Bob")
            val forest = CardInstance.create(forestDef, player1.id.value)

            val player1WithCard = player1.updateHand { it.addToTop(forest) }
            val gameState = GameState.newGame(player1WithCard, player2)

            val ecsState = StateConverter.toEcs(gameState)

            val hand = ecsState.getHand(EntityId.of("player1"))
            hand.size shouldBe 1

            val forestId = EntityId.fromCardId(forest.id)
            val cardComponent = ecsState.getComponent<CardComponent>(forestId)
            cardComponent?.name shouldBe "Forest"
        }
    }

    context("EcsGameState to GameState") {
        test("converts back to players") {
            val gameState = createTwoPlayerGame()
            val ecsState = StateConverter.toEcs(gameState)

            val backToGameState = StateConverter.fromEcs(ecsState)

            backToGameState.players.size shouldBe 2
            val player = backToGameState.getPlayer(gameState.players.keys.first())
            player.name shouldBe "Alice"
            player.life shouldBe 20
        }

        test("converts back cards on battlefield") {
            val player1 = Player.create(PlayerId.of("player1"), "Alice")
            val player2 = Player.create(PlayerId.of("player2"), "Bob")
            val bear = CardInstance.create(bearDef, player1.id.value)

            val gameState = GameState.newGame(player1, player2)
                .updateBattlefield { it.addToTop(bear) }
            val ecsState = StateConverter.toEcs(gameState)

            val backToGameState = StateConverter.fromEcs(ecsState)

            backToGameState.battlefield.size shouldBe 1
            val card = backToGameState.battlefield.cards.first()
            card.name shouldBe "Grizzly Bears"
        }
    }

    context("round-trip conversion") {
        test("preserves basic game state") {
            val gameState = createTwoPlayerGame()

            val roundTripped = StateConverter.fromEcs(StateConverter.toEcs(gameState))

            roundTripped.players.size shouldBe gameState.players.size
            roundTripped.turnState shouldBe gameState.turnState
            roundTripped.isGameOver shouldBe gameState.isGameOver
        }

        test("preserves player state") {
            val player1 = Player.create(PlayerId.of("player1"), "Alice")
                .loseLife(5)
                .addPoisonCounters(2)
                .recordLandPlayed()
            val player2 = Player.create(PlayerId.of("player2"), "Bob")

            val gameState = GameState.newGame(player1, player2)

            val roundTripped = StateConverter.fromEcs(StateConverter.toEcs(gameState))

            val p1 = roundTripped.getPlayer(player1.id)
            p1.life shouldBe 15
            p1.poisonCounters shouldBe 2
            p1.landsPlayedThisTurn shouldBe 1
        }

        test("preserves card state") {
            val player1 = Player.create(PlayerId.of("player1"), "Alice")
            val player2 = Player.create(PlayerId.of("player2"), "Bob")
            val bear = CardInstance.create(bearDef, player1.id.value)
                .tap()
                .dealDamage(1)
                .addCounter(CounterType.PLUS_ONE_PLUS_ONE, 2)
                .addKeyword(Keyword.FLYING)
                .modifyPower(1)

            val gameState = GameState.newGame(player1, player2)
                .updateBattlefield { it.addToTop(bear) }

            val roundTripped = StateConverter.fromEcs(StateConverter.toEcs(gameState))

            val card = roundTripped.battlefield.cards.first()
            card.isTapped.shouldBeTrue()
            card.damageMarked shouldBe 1
            card.counters[CounterType.PLUS_ONE_PLUS_ONE] shouldBe 2
            card.additionalKeywords.contains(Keyword.FLYING).shouldBeTrue()
            card.powerModifier shouldBe 1
        }

        test("preserves cards in all zones") {
            val player1 = Player.create(PlayerId.of("player1"), "Alice")
            val player2 = Player.create(PlayerId.of("player2"), "Bob")

            val libraryCard = CardInstance.create(forestDef, player1.id.value)
            val handCard = CardInstance.create(bearDef, player1.id.value)
            val graveyardCard = CardInstance.create(forestDef, player1.id.value)
            val battlefieldCard = CardInstance.create(bearDef, player1.id.value)
            val exileCard = CardInstance.create(forestDef, player1.id.value)

            val p1 = player1
                .updateLibrary { it.addToTop(libraryCard) }
                .updateHand { it.addToTop(handCard) }
                .updateGraveyard { it.addToTop(graveyardCard) }

            val gameState = GameState.newGame(p1, player2)
                .updateBattlefield { it.addToTop(battlefieldCard) }
                .updateExile { it.addToTop(exileCard) }

            val roundTripped = StateConverter.fromEcs(StateConverter.toEcs(gameState))

            val p1After = roundTripped.getPlayer(player1.id)
            p1After.library.size shouldBe 1
            p1After.hand.size shouldBe 1
            p1After.graveyard.size shouldBe 1
            roundTripped.battlefield.size shouldBe 1
            roundTripped.exile.size shouldBe 1
        }

        test("preserves attachment relationship") {
            val player1 = Player.create(PlayerId.of("player1"), "Alice")
            val player2 = Player.create(PlayerId.of("player2"), "Bob")

            val bear = CardInstance.create(bearDef, player1.id.value)
            val equipmentDef = CardDefinition.artifact(
                name = "Short Sword",
                manaCost = ManaCost.parse("{1}")
            )
            val equipment = CardInstance.create(equipmentDef, player1.id.value)
                .attachTo(bear.id)

            val gameState = GameState.newGame(player1, player2)
                .updateBattlefield { it.addToTop(bear).addToTop(equipment) }

            val roundTripped = StateConverter.fromEcs(StateConverter.toEcs(gameState))

            val roundTrippedEquipment = roundTripped.battlefield.cards.find { it.name == "Short Sword" }
            roundTrippedEquipment?.attachedTo shouldBe bear.id
        }

        test("preserves win/lose state") {
            val player1 = Player.create(PlayerId.of("player1"), "Alice").markAsWon()
            val player2 = Player.create(PlayerId.of("player2"), "Bob").markAsLost()

            val gameState = GameState.newGame(player1, player2)

            val roundTripped = StateConverter.fromEcs(StateConverter.toEcs(gameState))

            roundTripped.getPlayer(player1.id).hasWon.shouldBeTrue()
            roundTripped.getPlayer(player2.id).hasLost.shouldBeTrue()
        }
    }
})
