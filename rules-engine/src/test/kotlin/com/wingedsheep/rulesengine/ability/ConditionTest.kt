package com.wingedsheep.rulesengine.ability

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.combat.CombatState
import com.wingedsheep.rulesengine.core.CardId
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.game.GameState
import com.wingedsheep.rulesengine.player.Player
import com.wingedsheep.rulesengine.player.PlayerId
import com.wingedsheep.rulesengine.zone.Zone
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ConditionTest : FunSpec({

    val player1Id = PlayerId.of("player1")
    val player2Id = PlayerId.of("player2")

    fun createPlayer1(life: Int = 20) = Player.create(player1Id, "Alice").copy(life = life)
    fun createPlayer2(life: Int = 20) = Player.create(player2Id, "Bob").copy(life = life)

    val bearDef = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAST),
        power = 2,
        toughness = 2
    )

    val flyingDragonDef = CardDefinition.creature(
        name = "Shivan Dragon",
        manaCost = ManaCost.parse("{4}{R}{R}"),
        subtypes = setOf(Subtype.DRAGON),
        power = 5,
        toughness = 5,
        keywords = setOf(Keyword.FLYING)
    )

    context("Life Total Conditions") {

        test("LifeTotalAtMost is met when life is at or below threshold") {
            val state = GameState.newGame(createPlayer1(life = 10), createPlayer2())
            LifeTotalAtMost(10).isMet(state, player1Id) shouldBe true
            LifeTotalAtMost(15).isMet(state, player1Id) shouldBe true
            LifeTotalAtMost(5).isMet(state, player1Id) shouldBe false
        }

        test("LifeTotalAtLeast is met when life is at or above threshold") {
            val state = GameState.newGame(createPlayer1(life = 15), createPlayer2())
            LifeTotalAtLeast(15).isMet(state, player1Id) shouldBe true
            LifeTotalAtLeast(10).isMet(state, player1Id) shouldBe true
            LifeTotalAtLeast(20).isMet(state, player1Id) shouldBe false
        }

        test("MoreLifeThanOpponent is met when you have more life") {
            val state = GameState.newGame(createPlayer1(life = 25), createPlayer2(life = 20))
            MoreLifeThanOpponent.isMet(state, player1Id) shouldBe true
            MoreLifeThanOpponent.isMet(state, player2Id) shouldBe false
        }

        test("LessLifeThanOpponent is met when you have less life") {
            val state = GameState.newGame(createPlayer1(life = 15), createPlayer2(life = 20))
            LessLifeThanOpponent.isMet(state, player1Id) shouldBe true
            LessLifeThanOpponent.isMet(state, player2Id) shouldBe false
        }
    }

    context("Battlefield Conditions") {

        test("ControlCreature is met when you control a creature") {
            val bear = CardInstance.create(bearDef, player1Id.value)
            val state = GameState.newGame(createPlayer1(), createPlayer2())
                .copy(battlefield = Zone.battlefield(listOf(bear)))

            ControlCreature.isMet(state, player1Id) shouldBe true
            ControlCreature.isMet(state, player2Id) shouldBe false
        }

        test("ControlCreaturesAtLeast is met when you control enough creatures") {
            val bear1 = CardInstance.create(bearDef, player1Id.value)
            val bear2 = CardInstance.create(bearDef, player1Id.value)
            val state = GameState.newGame(createPlayer1(), createPlayer2())
                .copy(battlefield = Zone.battlefield(listOf(bear1, bear2)))

            ControlCreaturesAtLeast(2).isMet(state, player1Id) shouldBe true
            ControlCreaturesAtLeast(3).isMet(state, player1Id) shouldBe false
        }

        test("ControlCreatureWithKeyword is met when you control a creature with that keyword") {
            val dragon = CardInstance.create(flyingDragonDef, player1Id.value)
            val state = GameState.newGame(createPlayer1(), createPlayer2())
                .copy(battlefield = Zone.battlefield(listOf(dragon)))

            ControlCreatureWithKeyword(Keyword.FLYING).isMet(state, player1Id) shouldBe true
            ControlCreatureWithKeyword(Keyword.TRAMPLE).isMet(state, player1Id) shouldBe false
        }

        test("ControlCreatureOfType is met when you control a creature of that type") {
            val dragon = CardInstance.create(flyingDragonDef, player1Id.value)
            val state = GameState.newGame(createPlayer1(), createPlayer2())
                .copy(battlefield = Zone.battlefield(listOf(dragon)))

            ControlCreatureOfType(Subtype.DRAGON).isMet(state, player1Id) shouldBe true
            ControlCreatureOfType(Subtype.BEAST).isMet(state, player1Id) shouldBe false
        }

        test("OpponentControlsCreature is met when opponent has a creature") {
            val bear = CardInstance.create(bearDef, player2Id.value)
            val state = GameState.newGame(createPlayer1(), createPlayer2())
                .copy(battlefield = Zone.battlefield(listOf(bear)))

            OpponentControlsCreature.isMet(state, player1Id) shouldBe true
            OpponentControlsCreature.isMet(state, player2Id) shouldBe false
        }

        test("OpponentControlsMoreCreatures is met when opponent has more creatures") {
            val bear1 = CardInstance.create(bearDef, player1Id.value)
            val bear2 = CardInstance.create(bearDef, player2Id.value)
            val bear3 = CardInstance.create(bearDef, player2Id.value)
            val state = GameState.newGame(createPlayer1(), createPlayer2())
                .copy(battlefield = Zone.battlefield(listOf(bear1, bear2, bear3)))

            OpponentControlsMoreCreatures.isMet(state, player1Id) shouldBe true
            OpponentControlsMoreCreatures.isMet(state, player2Id) shouldBe false
        }
    }

    context("Hand/Library Conditions") {

        test("EmptyHand is met when hand is empty") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            // New game has empty hand
            EmptyHand.isMet(state, player1Id) shouldBe true

            // Add a card to hand
            val card = CardInstance.create(bearDef, player1Id.value)
            val stateWithCard = state.updatePlayer(player1Id) { player ->
                player.copy(hand = player.hand.addToTop(card))
            }
            EmptyHand.isMet(stateWithCard, player1Id) shouldBe false
        }

        test("CardsInHandAtLeast is met when you have enough cards") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())

            // Add cards to hand
            val cards = (1..5).map { CardInstance.create(bearDef, player1Id.value) }
            val stateWithCards = state.updatePlayer(player1Id) { player ->
                player.copy(hand = cards.fold(player.hand) { hand, card -> hand.addToTop(card) })
            }

            CardsInHandAtLeast(5).isMet(stateWithCards, player1Id) shouldBe true
            CardsInHandAtLeast(7).isMet(stateWithCards, player1Id) shouldBe false
        }

        test("CardsInHandAtMost is met when you have few enough cards") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())

            // Add cards to hand
            val cards = (1..5).map { CardInstance.create(bearDef, player1Id.value) }
            val stateWithCards = state.updatePlayer(player1Id) { player ->
                player.copy(hand = cards.fold(player.hand) { hand, card -> hand.addToTop(card) })
            }

            CardsInHandAtMost(5).isMet(stateWithCards, player1Id) shouldBe true
            CardsInHandAtMost(3).isMet(stateWithCards, player1Id) shouldBe false
        }
    }

    context("Source Conditions") {

        test("SourceIsAttacking is met when source is attacking") {
            val bear = CardInstance.create(bearDef, player1Id.value)
            @Suppress("DEPRECATION")
            val combat = CombatState.fromPlayerIds(player1Id, player2Id).addAttacker(bear.id)
            val state = GameState.newGame(createPlayer1(), createPlayer2())
                .copy(
                    battlefield = Zone.battlefield(listOf(bear)),
                    combat = combat
                )

            SourceIsAttacking.isMet(state, player1Id, bear.id) shouldBe true
            SourceIsAttacking.isMet(state, player1Id, CardId.generate()) shouldBe false
            SourceIsAttacking.isMet(state, player1Id, null) shouldBe false
        }

        test("SourceIsTapped is met when source is tapped") {
            val bear = CardInstance.create(bearDef, player1Id.value).tap()
            val state = GameState.newGame(createPlayer1(), createPlayer2())
                .copy(battlefield = Zone.battlefield(listOf(bear)))

            SourceIsTapped.isMet(state, player1Id, bear.id) shouldBe true
            SourceIsUntapped.isMet(state, player1Id, bear.id) shouldBe false
        }
    }

    context("Turn/Phase Conditions") {

        test("IsYourTurn is met when it's your turn") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            // Player 1 is active player in new game
            IsYourTurn.isMet(state, player1Id) shouldBe true
            IsYourTurn.isMet(state, player2Id) shouldBe false
        }

        test("IsNotYourTurn is met when it's not your turn") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            IsNotYourTurn.isMet(state, player1Id) shouldBe false
            IsNotYourTurn.isMet(state, player2Id) shouldBe true
        }
    }

    context("Composite Conditions") {

        test("AllConditions requires all conditions to be met") {
            val state = GameState.newGame(createPlayer1(life = 10), createPlayer2(life = 20))

            val condition = AllConditions(listOf(
                LifeTotalAtMost(15),
                LessLifeThanOpponent
            ))

            condition.isMet(state, player1Id) shouldBe true

            val stateHighLife = GameState.newGame(createPlayer1(life = 25), createPlayer2(life = 20))
            condition.isMet(stateHighLife, player1Id) shouldBe false
        }

        test("AnyCondition requires at least one condition to be met") {
            val state = GameState.newGame(createPlayer1(life = 25), createPlayer2(life = 20))

            val condition = AnyCondition(listOf(
                LifeTotalAtMost(10),
                MoreLifeThanOpponent
            ))

            condition.isMet(state, player1Id) shouldBe true

            val stateLowLife = GameState.newGame(createPlayer1(life = 5), createPlayer2(life = 20))
            condition.isMet(stateLowLife, player1Id) shouldBe true
        }

        test("NotCondition inverts the result") {
            val state = GameState.newGame(createPlayer1(life = 10), createPlayer2())

            val condition = NotCondition(LifeTotalAtLeast(15))
            condition.isMet(state, player1Id) shouldBe true

            val stateHighLife = GameState.newGame(createPlayer1(life = 20), createPlayer2())
            condition.isMet(stateHighLife, player1Id) shouldBe false
        }
    }

    context("ConditionalEffect") {

        test("ConditionalEffect has correct description") {
            val effect = ConditionalEffect(
                condition = LifeTotalAtMost(10),
                effect = DrawCardsEffect(2),
                elseEffect = DrawCardsEffect(1)
            )

            effect.description shouldBe "If your life total is 10 or less, draw 2 cards. Otherwise, draw a card"
        }

        test("ConditionalEffect without else clause") {
            val effect = ConditionalEffect(
                condition = ControlCreature,
                effect = GainLifeEffect(3)
            )

            effect.description shouldBe "If you control a creature, you gain 3 life"
        }
    }

    context("Condition descriptions") {

        test("All conditions have readable descriptions") {
            LifeTotalAtMost(10).description shouldBe "if your life total is 10 or less"
            LifeTotalAtLeast(10).description shouldBe "if your life total is 10 or more"
            MoreLifeThanOpponent.description shouldBe "if you have more life than an opponent"
            ControlCreature.description shouldBe "if you control a creature"
            ControlCreaturesAtLeast(3).description shouldBe "if you control 3 or more creatures"
            EmptyHand.description shouldBe "if you have no cards in hand"
            IsYourTurn.description shouldBe "if it's your turn"
            SourceIsAttacking.description shouldBe "if this creature is attacking"
        }
    }
})
