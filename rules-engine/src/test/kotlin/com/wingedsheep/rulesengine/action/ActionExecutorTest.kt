package com.wingedsheep.rulesengine.action

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.card.CounterType
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.game.GameState
import com.wingedsheep.rulesengine.player.Player
import com.wingedsheep.rulesengine.player.PlayerId
import com.wingedsheep.rulesengine.zone.ZoneType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ActionExecutorTest : FunSpec({

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

    val forestDef = CardDefinition.basicLand(
        name = "Forest",
        subtype = Subtype.FOREST
    )

    fun createGameWithBearOnBattlefield(): Pair<GameState, CardInstance> {
        val state = GameState.newGame(createPlayer1(), createPlayer2())
        val bear = CardInstance.create(bearDef, player1Id.value).copy(
            controllerId = player1Id.value,
            summoningSickness = false
        )
        return state.updateBattlefield { it.addToTop(bear) } to bear
    }

    fun createGameWithCardInHand(): Pair<GameState, CardInstance> {
        val state = GameState.newGame(createPlayer1(), createPlayer2())
        val card = CardInstance.create(bearDef, player1Id.value)
        val newState = state.updatePlayer(player1Id) { p ->
            p.updateHand { it.addToTop(card) }
        }
        return newState to card
    }

    fun createGameWithCardInLibrary(): Pair<GameState, CardInstance> {
        val state = GameState.newGame(createPlayer1(), createPlayer2())
        val card = CardInstance.create(bearDef, player1Id.value)
        val newState = state.updatePlayer(player1Id) { p ->
            p.updateLibrary { it.addToTop(card) }
        }
        return newState to card
    }

    context("life actions") {
        test("GainLife increases player life") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val action = GainLife(player1Id, 5)

            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.getPlayer(player1Id).life shouldBe 25
        }

        test("GainLife generates LifeChanged event") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val action = GainLife(player1Id, 5)

            val result = ActionExecutor.execute(state, action) as ActionResult.Success

            result.events shouldHaveSize 1
            val event = result.events.first()
            event.shouldBeInstanceOf<GameEvent.LifeChanged>()
            event.playerId shouldBe player1Id.value
            event.oldLife shouldBe 20
            event.newLife shouldBe 25
            event.delta shouldBe 5
        }

        test("LoseLife decreases player life") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val action = LoseLife(player1Id, 3)

            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.getPlayer(player1Id).life shouldBe 17
        }

        test("LoseLife can result in negative life") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val action = LoseLife(player1Id, 25)

            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.getPlayer(player1Id).life shouldBe -5
        }

        test("SetLife sets player life to exact amount") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val action = SetLife(player1Id, 10)

            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.getPlayer(player1Id).life shouldBe 10
        }

        test("DealDamage reduces player life") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val action = DealDamage(player1Id, 5)

            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.getPlayer(player1Id).life shouldBe 15
        }

        test("DealDamage generates DamageDealt and LifeChanged events") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val action = DealDamage(player1Id, 5)

            val result = ActionExecutor.execute(state, action) as ActionResult.Success

            result.events shouldHaveSize 2
            result.events.any { it is GameEvent.DamageDealt } shouldBe true
            result.events.any { it is GameEvent.LifeChanged } shouldBe true
        }
    }

    context("mana actions") {
        test("AddMana adds colored mana to pool") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val action = AddMana(player1Id, Color.GREEN, 2)

            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.getPlayer(player1Id).manaPool.get(Color.GREEN) shouldBe 2
        }

        test("AddMana generates ManaAdded event") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val action = AddMana(player1Id, Color.RED, 1)

            val result = ActionExecutor.execute(state, action) as ActionResult.Success

            result.events shouldHaveSize 1
            val event = result.events.first()
            event.shouldBeInstanceOf<GameEvent.ManaAdded>()
            event.playerId shouldBe player1Id.value
            event.color shouldBe "Red"
            event.amount shouldBe 1
        }

        test("AddColorlessMana adds colorless mana") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val action = AddColorlessMana(player1Id, 3)

            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.getPlayer(player1Id).manaPool.colorless shouldBe 3
        }

        test("EmptyManaPool clears all mana") {
            var state = GameState.newGame(createPlayer1(), createPlayer2())
            state = state.updatePlayer(player1Id) { it.addMana(Color.GREEN, 3).addColorlessMana(2) }

            val action = EmptyManaPool(player1Id)
            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.getPlayer(player1Id).manaPool.isEmpty.shouldBeTrue()
        }
    }

    context("card drawing") {
        test("DrawCard moves card from library to hand") {
            val (state, card) = createGameWithCardInLibrary()
            val action = DrawCard(player1Id, 1)

            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.getPlayer(player1Id).hand.contains(card.id).shouldBeTrue()
            result.state.getPlayer(player1Id).library.contains(card.id).shouldBeFalse()
        }

        test("DrawCard generates CardDrawn event") {
            val (state, card) = createGameWithCardInLibrary()
            val action = DrawCard(player1Id, 1)

            val result = ActionExecutor.execute(state, action) as ActionResult.Success

            result.events shouldHaveSize 1
            val event = result.events.first()
            event.shouldBeInstanceOf<GameEvent.CardDrawn>()
            event.playerId shouldBe player1Id.value
            event.cardId shouldBe card.id.value
        }

        test("DrawCard can draw multiple cards") {
            var state = GameState.newGame(createPlayer1(), createPlayer2())
            val card1 = CardInstance.create(bearDef, player1Id.value)
            val card2 = CardInstance.create(bearDef, player1Id.value)
            state = state.updatePlayer(player1Id) { p ->
                p.updateLibrary { it.addToTop(card1).addToTop(card2) }
            }

            val action = DrawCard(player1Id, 2)
            val result = ActionExecutor.execute(state, action) as ActionResult.Success

            result.state.getPlayer(player1Id).handSize shouldBe 2
            result.events shouldHaveSize 2
        }

        test("DrawCard from empty library draws nothing") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val action = DrawCard(player1Id, 1)

            val result = ActionExecutor.execute(state, action) as ActionResult.Success

            result.state.getPlayer(player1Id).handSize shouldBe 0
            result.events shouldHaveSize 0
        }

        test("DrawSpecificCard draws specific card from library") {
            val (state, card) = createGameWithCardInLibrary()
            val action = DrawSpecificCard(player1Id, card.id)

            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.getPlayer(player1Id).hand.contains(card.id).shouldBeTrue()
        }

        test("DrawSpecificCard fails if card not in library") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val card = CardInstance.create(bearDef, player1Id.value)
            val action = DrawSpecificCard(player1Id, card.id)

            val result = ActionExecutor.execute(state, action)

            result.isFailure.shouldBeTrue()
        }
    }

    context("library actions") {
        test("ShuffleLibrary shuffles the library") {
            var state = GameState.newGame(createPlayer1(), createPlayer2())
            repeat(10) {
                val card = CardInstance.create(bearDef, player1Id.value)
                state = state.updatePlayer(player1Id) { p ->
                    p.updateLibrary { it.addToTop(card) }
                }
            }

            val action = ShuffleLibrary(player1Id)
            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.getPlayer(player1Id).librarySize shouldBe 10
        }

        test("PutCardOnTopOfLibrary moves card to top of library") {
            val (state, bear) = createGameWithBearOnBattlefield()
            val action = PutCardOnTopOfLibrary(bear.id, player1Id)

            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.battlefield.contains(bear.id).shouldBeFalse()
            result.state.getPlayer(player1Id).library.cards.firstOrNull()?.id shouldBe bear.id
        }

        test("PutCardOnBottomOfLibrary moves card to bottom of library") {
            val (state, bear) = createGameWithBearOnBattlefield()
            val action = PutCardOnBottomOfLibrary(bear.id, player1Id)

            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.battlefield.contains(bear.id).shouldBeFalse()
            result.state.getPlayer(player1Id).library.cards.lastOrNull()?.id shouldBe bear.id
        }
    }

    context("card movement") {
        test("MoveCard moves card between zones") {
            val (state, card) = createGameWithCardInHand()
            val action = MoveCard(
                cardId = card.id,
                fromZone = ZoneType.HAND,
                toZone = ZoneType.GRAVEYARD,
                fromOwnerId = player1Id,
                toOwnerId = player1Id
            )

            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.getPlayer(player1Id).hand.contains(card.id).shouldBeFalse()
            result.state.getPlayer(player1Id).graveyard.contains(card.id).shouldBeTrue()
        }

        test("MoveCard generates CardMoved event") {
            val (state, card) = createGameWithCardInHand()
            val action = MoveCard(
                cardId = card.id,
                fromZone = ZoneType.HAND,
                toZone = ZoneType.BATTLEFIELD,
                fromOwnerId = player1Id
            )

            val result = ActionExecutor.execute(state, action) as ActionResult.Success

            result.events.any { it is GameEvent.CardMoved } shouldBe true
        }

        test("PutCardOntoBattlefield puts card on battlefield") {
            val (state, card) = createGameWithCardInHand()
            val action = PutCardOntoBattlefield(card.id, player1Id)

            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.battlefield.contains(card.id).shouldBeTrue()
            result.state.getPlayer(player1Id).hand.contains(card.id).shouldBeFalse()
        }

        test("PutCardOntoBattlefield tapped puts card tapped") {
            val (state, card) = createGameWithCardInHand()
            val action = PutCardOntoBattlefield(card.id, player1Id, tapped = true)

            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.battlefield.getCard(card.id)!!.isTapped.shouldBeTrue()
        }

        test("PutCardOntoBattlefield gives creatures summoning sickness") {
            val (state, card) = createGameWithCardInHand()
            val action = PutCardOntoBattlefield(card.id, player1Id)

            val result = ActionExecutor.execute(state, action)

            result.state.battlefield.getCard(card.id)!!.summoningSickness.shouldBeTrue()
        }

        test("DestroyCard moves permanent to graveyard") {
            val (state, bear) = createGameWithBearOnBattlefield()
            val action = DestroyCard(bear.id)

            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.battlefield.contains(bear.id).shouldBeFalse()
            result.state.getPlayer(player1Id).graveyard.contains(bear.id).shouldBeTrue()
        }

        test("DestroyCard generates CreatureDied event for creatures") {
            val (state, bear) = createGameWithBearOnBattlefield()
            val action = DestroyCard(bear.id)

            val result = ActionExecutor.execute(state, action) as ActionResult.Success

            result.events.any { it is GameEvent.CreatureDied } shouldBe true
        }

        test("SacrificeCard moves permanent to graveyard") {
            val (state, bear) = createGameWithBearOnBattlefield()
            val action = SacrificeCard(bear.id, player1Id)

            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.battlefield.contains(bear.id).shouldBeFalse()
            result.state.getPlayer(player1Id).graveyard.contains(bear.id).shouldBeTrue()
        }

        test("ExileCard moves card to exile zone") {
            val (state, bear) = createGameWithBearOnBattlefield()
            val action = ExileCard(bear.id)

            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.battlefield.contains(bear.id).shouldBeFalse()
            result.state.exile.contains(bear.id).shouldBeTrue()
        }

        test("ReturnToHand moves card to owner's hand") {
            val (state, bear) = createGameWithBearOnBattlefield()
            val action = ReturnToHand(bear.id, player1Id)

            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.battlefield.contains(bear.id).shouldBeFalse()
            result.state.getPlayer(player1Id).hand.contains(bear.id).shouldBeTrue()
        }

        test("DiscardCard moves card from hand to graveyard") {
            val (state, card) = createGameWithCardInHand()
            val action = DiscardCard(card.id, player1Id)

            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.getPlayer(player1Id).hand.contains(card.id).shouldBeFalse()
            result.state.getPlayer(player1Id).graveyard.contains(card.id).shouldBeTrue()
        }

        test("DiscardCard fails if card not in hand") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val card = CardInstance.create(bearDef, player1Id.value)
            val action = DiscardCard(card.id, player1Id)

            val result = ActionExecutor.execute(state, action)

            result.isFailure.shouldBeTrue()
        }
    }

    context("tap/untap actions") {
        test("TapCard taps a permanent") {
            val (state, bear) = createGameWithBearOnBattlefield()
            val action = TapCard(bear.id)

            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.battlefield.getCard(bear.id)!!.isTapped.shouldBeTrue()
        }

        test("TapCard generates CardTapped event") {
            val (state, bear) = createGameWithBearOnBattlefield()
            val action = TapCard(bear.id)

            val result = ActionExecutor.execute(state, action) as ActionResult.Success

            result.events shouldHaveSize 1
            result.events.first().shouldBeInstanceOf<GameEvent.CardTapped>()
        }

        test("UntapCard untaps a permanent") {
            val (initialState, bear) = createGameWithBearOnBattlefield()
            val state = initialState.updateBattlefield { it.updateCard(bear.id) { c -> c.tap() } }
            val action = UntapCard(bear.id)

            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.battlefield.getCard(bear.id)!!.isTapped.shouldBeFalse()
        }

        test("UntapCard generates CardUntapped event") {
            val (initialState, bear) = createGameWithBearOnBattlefield()
            val state = initialState.updateBattlefield { it.updateCard(bear.id) { c -> c.tap() } }
            val action = UntapCard(bear.id)

            val result = ActionExecutor.execute(state, action) as ActionResult.Success

            result.events shouldHaveSize 1
            result.events.first().shouldBeInstanceOf<GameEvent.CardUntapped>()
        }

        test("UntapAllPermanents untaps all permanents controlled by player") {
            var state = GameState.newGame(createPlayer1(), createPlayer2())
            val bear1 = CardInstance.create(bearDef, player1Id.value).copy(
                controllerId = player1Id.value,
                isTapped = true
            )
            val bear2 = CardInstance.create(bearDef, player1Id.value).copy(
                controllerId = player1Id.value,
                isTapped = true
            )
            val opponentBear = CardInstance.create(bearDef, player2Id.value).copy(
                controllerId = player2Id.value,
                isTapped = true
            )
            state = state.updateBattlefield { it.addToTop(bear1).addToTop(bear2).addToTop(opponentBear) }

            val action = UntapAllPermanents(player1Id)
            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.battlefield.getCard(bear1.id)!!.isTapped.shouldBeFalse()
            result.state.battlefield.getCard(bear2.id)!!.isTapped.shouldBeFalse()
            result.state.battlefield.getCard(opponentBear.id)!!.isTapped.shouldBeTrue()
        }
    }

    context("combat damage") {
        test("DealCombatDamageToPlayer reduces player life") {
            val (state, bear) = createGameWithBearOnBattlefield()
            val action = DealCombatDamageToPlayer(bear.id, player2Id, 2)

            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.getPlayer(player2Id).life shouldBe 18
        }

        test("DealCombatDamageToCreature marks damage on creature") {
            var state = GameState.newGame(createPlayer1(), createPlayer2())
            val attacker = CardInstance.create(bearDef, player1Id.value).copy(
                controllerId = player1Id.value
            )
            val defender = CardInstance.create(bearDef, player2Id.value).copy(
                controllerId = player2Id.value
            )
            state = state.updateBattlefield { it.addToTop(attacker).addToTop(defender) }

            val action = DealCombatDamageToCreature(attacker.id, defender.id, 2)
            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.battlefield.getCard(defender.id)!!.damageMarked shouldBe 2
        }

        test("MarkDamageOnCreature marks damage") {
            val (state, bear) = createGameWithBearOnBattlefield()
            val action = MarkDamageOnCreature(bear.id, 1)

            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.battlefield.getCard(bear.id)!!.damageMarked shouldBe 1
        }

        test("ClearDamageFromCreature clears damage") {
            val (initialState, bear) = createGameWithBearOnBattlefield()
            val state = initialState.updateBattlefield { it.updateCard(bear.id) { c -> c.dealDamage(1) } }
            val action = ClearDamageFromCreature(bear.id)

            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.battlefield.getCard(bear.id)!!.damageMarked shouldBe 0
        }

        test("ClearAllDamage clears damage from all creatures") {
            var state = GameState.newGame(createPlayer1(), createPlayer2())
            val bear1 = CardInstance.create(bearDef, player1Id.value).copy(
                controllerId = player1Id.value,
                damageMarked = 1
            )
            val bear2 = CardInstance.create(bearDef, player2Id.value).copy(
                controllerId = player2Id.value,
                damageMarked = 1
            )
            state = state.updateBattlefield { it.addToTop(bear1).addToTop(bear2) }

            val action = ClearAllDamage()
            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.battlefield.getCard(bear1.id)!!.damageMarked shouldBe 0
            result.state.battlefield.getCard(bear2.id)!!.damageMarked shouldBe 0
        }
    }

    context("counters") {
        test("AddCounters adds counters to permanent") {
            val (state, bear) = createGameWithBearOnBattlefield()
            val action = AddCounters(bear.id, "PLUS_ONE_PLUS_ONE", 2)

            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.battlefield.getCard(bear.id)!!.counters[CounterType.PLUS_ONE_PLUS_ONE] shouldBe 2
        }

        test("AddCounters increases creature power/toughness with +1/+1 counters") {
            val (state, bear) = createGameWithBearOnBattlefield()
            val action = AddCounters(bear.id, "PLUS_ONE_PLUS_ONE", 1)

            val result = ActionExecutor.execute(state, action)

            val updatedBear = result.state.battlefield.getCard(bear.id)!!
            updatedBear.currentPower shouldBe 3
            updatedBear.currentToughness shouldBe 3
        }

        test("RemoveCounters removes counters from permanent") {
            val (initialState, bear) = createGameWithBearOnBattlefield()
            val state = initialState.updateBattlefield {
                it.updateCard(bear.id) { c -> c.addCounter(CounterType.PLUS_ONE_PLUS_ONE, 3) }
            }
            val action = RemoveCounters(bear.id, "PLUS_ONE_PLUS_ONE", 1)

            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.battlefield.getCard(bear.id)!!.counters[CounterType.PLUS_ONE_PLUS_ONE] shouldBe 2
        }

        test("AddPoisonCounters adds poison to player") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val action = AddPoisonCounters(player1Id, 3)

            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.getPlayer(player1Id).poisonCounters shouldBe 3
        }
    }

    context("summoning sickness") {
        test("RemoveSummoningSickness removes summoning sickness") {
            var state = GameState.newGame(createPlayer1(), createPlayer2())
            val bear = CardInstance.create(bearDef, player1Id.value).copy(
                controllerId = player1Id.value,
                summoningSickness = true
            )
            state = state.updateBattlefield { it.addToTop(bear) }

            val action = RemoveSummoningSickness(bear.id)
            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.battlefield.getCard(bear.id)!!.summoningSickness.shouldBeFalse()
        }

        test("RemoveSummoningSicknessFromAll removes from all creatures") {
            var state = GameState.newGame(createPlayer1(), createPlayer2())
            val bear1 = CardInstance.create(bearDef, player1Id.value).copy(
                controllerId = player1Id.value,
                summoningSickness = true
            )
            val bear2 = CardInstance.create(bearDef, player1Id.value).copy(
                controllerId = player1Id.value,
                summoningSickness = true
            )
            val opponentBear = CardInstance.create(bearDef, player2Id.value).copy(
                controllerId = player2Id.value,
                summoningSickness = true
            )
            state = state.updateBattlefield { it.addToTop(bear1).addToTop(bear2).addToTop(opponentBear) }

            val action = RemoveSummoningSicknessFromAll(player1Id)
            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.battlefield.getCard(bear1.id)!!.summoningSickness.shouldBeFalse()
            result.state.battlefield.getCard(bear2.id)!!.summoningSickness.shouldBeFalse()
            result.state.battlefield.getCard(opponentBear.id)!!.summoningSickness.shouldBeTrue()
        }
    }

    context("land actions") {
        test("PlayLand moves land from hand to battlefield") {
            var state = GameState.newGame(createPlayer1(), createPlayer2())
            val forest = CardInstance.create(forestDef, player1Id.value)
            state = state.updatePlayer(player1Id) { p -> p.updateHand { it.addToTop(forest) } }

            val action = PlayLand(forest.id, player1Id)
            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.battlefield.contains(forest.id).shouldBeTrue()
            result.state.getPlayer(player1Id).hand.contains(forest.id).shouldBeFalse()
            result.state.getPlayer(player1Id).landsPlayedThisTurn shouldBe 1
        }

        test("PlayLand fails if player already played land this turn") {
            var state = GameState.newGame(createPlayer1(), createPlayer2())
            state = state.updatePlayer(player1Id) { p -> p.copy(landsPlayedThisTurn = 1) }
            val forest = CardInstance.create(forestDef, player1Id.value)
            state = state.updatePlayer(player1Id) { p -> p.updateHand { it.addToTop(forest) } }

            val action = PlayLand(forest.id, player1Id)
            val result = ActionExecutor.execute(state, action)

            result.isFailure.shouldBeTrue()
            (result as ActionResult.Failure).reason shouldBe "Cannot play another land this turn"
        }

        test("PlayLand fails if card is not a land") {
            val (state, card) = createGameWithCardInHand()
            val action = PlayLand(card.id, player1Id)

            val result = ActionExecutor.execute(state, action)

            result.isFailure.shouldBeTrue()
            (result as ActionResult.Failure).reason shouldBe "Card is not a land"
        }

        test("ResetLandsPlayed resets lands played counter") {
            var state = GameState.newGame(createPlayer1(), createPlayer2())
            state = state.updatePlayer(player1Id) { p -> p.copy(landsPlayedThisTurn = 1) }

            val action = ResetLandsPlayed(player1Id)
            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.getPlayer(player1Id).landsPlayedThisTurn shouldBe 0
        }
    }

    context("game flow") {
        test("EndGame marks game as over") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val action = EndGame(player1Id)

            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.isGameOver.shouldBeTrue()
            result.state.winner shouldBe player1Id
        }

        test("EndGame with null winner is a draw") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val action = EndGame(null)

            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.isGameOver.shouldBeTrue()
            result.state.winner.shouldBeNull()
        }

        test("EndGame generates GameEnded event") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val action = EndGame(player1Id)

            val result = ActionExecutor.execute(state, action) as ActionResult.Success

            result.events shouldHaveSize 1
            result.events.first().shouldBeInstanceOf<GameEvent.GameEnded>()
        }

        test("PlayerLoses marks player as lost") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val action = PlayerLoses(player1Id, "Life total reached 0")

            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.getPlayer(player1Id).hasLost.shouldBeTrue()
        }

        test("PlayerLoses generates PlayerLost event") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val action = PlayerLoses(player1Id, "Life total reached 0")

            val result = ActionExecutor.execute(state, action) as ActionResult.Success

            result.events shouldHaveSize 1
            val event = result.events.first()
            event.shouldBeInstanceOf<GameEvent.PlayerLost>()
            event.playerId shouldBe player1Id.value
            event.reason shouldBe "Life total reached 0"
        }
    }

    context("executeAll") {
        test("executeAll executes multiple actions in sequence") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val actions = listOf(
                GainLife(player1Id, 5),
                LoseLife(player2Id, 3),
                AddMana(player1Id, Color.GREEN, 2)
            )

            val result = ActionExecutor.executeAll(state, actions)

            result.isSuccess.shouldBeTrue()
            result.state.getPlayer(player1Id).life shouldBe 25
            result.state.getPlayer(player2Id).life shouldBe 17
            result.state.getPlayer(player1Id).manaPool.get(Color.GREEN) shouldBe 2
        }

        test("executeAll stops on first failure") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val card = CardInstance.create(bearDef, player1Id.value)
            val actions = listOf(
                GainLife(player1Id, 5),
                DiscardCard(card.id, player1Id), // This will fail - card not in hand
                LoseLife(player2Id, 3) // This should not execute
            )

            val result = ActionExecutor.executeAll(state, actions)

            result.isFailure.shouldBeTrue()
            result.state.getPlayer(player1Id).life shouldBe 25 // First action applied
            result.state.getPlayer(player2Id).life shouldBe 20 // Third action not applied
        }

        test("executeAll accumulates events from all actions") {
            val (state, card) = createGameWithCardInLibrary()
            val actions = listOf(
                GainLife(player1Id, 5),
                DrawCard(player1Id, 1)
            )

            val result = ActionExecutor.executeAll(state, actions) as ActionResult.Success

            result.events shouldHaveSize 2
            result.events.any { it is GameEvent.LifeChanged } shouldBe true
            result.events.any { it is GameEvent.CardDrawn } shouldBe true
        }
    }

    context("action descriptions") {
        test("GainLife has correct description") {
            val action = GainLife(player1Id, 5)
            action.description shouldBe "player1 gains 5 life"
        }

        test("LoseLife has correct description") {
            val action = LoseLife(player1Id, 3)
            action.description shouldBe "player1 loses 3 life"
        }

        test("DrawCard has correct description") {
            val action = DrawCard(player1Id, 2)
            action.description shouldBe "player1 draws 2 card(s)"
        }
    }
})
