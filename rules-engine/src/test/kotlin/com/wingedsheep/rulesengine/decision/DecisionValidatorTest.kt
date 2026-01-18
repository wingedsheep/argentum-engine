package com.wingedsheep.rulesengine.decision

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.core.CardId
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.game.GameState
import com.wingedsheep.rulesengine.player.Player
import com.wingedsheep.rulesengine.player.PlayerId
import com.wingedsheep.rulesengine.targeting.Target
import com.wingedsheep.rulesengine.targeting.TargetCreature
import com.wingedsheep.rulesengine.targeting.TargetPlayer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class DecisionValidatorTest : FunSpec({

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

    context("validateTargetsChoice") {
        test("valid targets are accepted") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val bear = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value)

            val decision = ChooseTargets(
                playerId = player1Id,
                sourceCardId = CardId("source"),
                sourceName = "Lightning Bolt",
                requirements = listOf(TargetCreature()),
                legalTargets = mapOf(0 to listOf(Target.card(bear.id)))
            )

            val response = TargetsChoice(mapOf(0 to listOf(Target.card(bear.id))))
            val result = DecisionValidator.validate(state, decision, response)

            result.shouldBeInstanceOf<DecisionValidator.ValidationResult.Valid>()
        }

        test("invalid target is rejected") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val bear = CardInstance.create(bearDef, player1Id.value)
            val otherBear = CardInstance.create(bearDef, player1Id.value)

            val decision = ChooseTargets(
                playerId = player1Id,
                sourceCardId = CardId("source"),
                sourceName = "Lightning Bolt",
                requirements = listOf(TargetCreature()),
                legalTargets = mapOf(0 to listOf(Target.card(bear.id)))
            )

            // Select a target that isn't in the legal targets list
            val response = TargetsChoice(mapOf(0 to listOf(Target.card(otherBear.id))))
            val result = DecisionValidator.validate(state, decision, response)

            result.shouldBeInstanceOf<DecisionValidator.ValidationResult.Invalid>()
        }

        test("wrong number of targets is rejected") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val bear = CardInstance.create(bearDef, player1Id.value)

            val decision = ChooseTargets(
                playerId = player1Id,
                sourceCardId = CardId("source"),
                sourceName = "Lightning Bolt",
                requirements = listOf(TargetCreature(count = 2)), // Requires 2 targets
                legalTargets = mapOf(0 to listOf(Target.card(bear.id)))
            )

            // Only select 1 target
            val response = TargetsChoice(mapOf(0 to listOf(Target.card(bear.id))))
            val result = DecisionValidator.validate(state, decision, response)

            result.shouldBeInstanceOf<DecisionValidator.ValidationResult.Invalid>()
        }
    }

    context("validateAttackersChoice") {
        test("valid attackers are accepted") {
            val bear1 = CardInstance.create(bearDef, player1Id.value)
            val bear2 = CardInstance.create(bearDef, player1Id.value)

            val decision = ChooseAttackers(
                playerId = player1Id,
                legalAttackers = listOf(bear1.id, bear2.id),
                defendingPlayer = player2Id
            )

            val response = AttackersChoice(listOf(bear1.id))
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val result = DecisionValidator.validate(state, decision, response)

            result.shouldBeInstanceOf<DecisionValidator.ValidationResult.Valid>()
        }

        test("illegal attacker is rejected") {
            val bear1 = CardInstance.create(bearDef, player1Id.value)
            val bear2 = CardInstance.create(bearDef, player1Id.value)

            val decision = ChooseAttackers(
                playerId = player1Id,
                legalAttackers = listOf(bear1.id), // Only bear1 can attack
                defendingPlayer = player2Id
            )

            val response = AttackersChoice(listOf(bear2.id)) // Try to attack with bear2
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val result = DecisionValidator.validate(state, decision, response)

            result.shouldBeInstanceOf<DecisionValidator.ValidationResult.Invalid>()
        }

        test("duplicate attackers are rejected") {
            val bear = CardInstance.create(bearDef, player1Id.value)

            val decision = ChooseAttackers(
                playerId = player1Id,
                legalAttackers = listOf(bear.id),
                defendingPlayer = player2Id
            )

            val response = AttackersChoice(listOf(bear.id, bear.id))
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val result = DecisionValidator.validate(state, decision, response)

            result.shouldBeInstanceOf<DecisionValidator.ValidationResult.Invalid>()
        }
    }

    context("validateBlockersChoice") {
        test("valid blocks are accepted") {
            val attacker = CardInstance.create(bearDef, player1Id.value)
            val blocker = CardInstance.create(bearDef, player2Id.value)

            val decision = ChooseBlockers(
                playerId = player2Id,
                legalBlockers = listOf(blocker.id),
                attackers = listOf(attacker.id),
                legalBlocks = mapOf(blocker.id to listOf(attacker.id))
            )

            val response = BlockersChoice(mapOf(blocker.id to attacker.id))
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val result = DecisionValidator.validate(state, decision, response)

            result.shouldBeInstanceOf<DecisionValidator.ValidationResult.Valid>()
        }

        test("illegal block is rejected") {
            val attacker = CardInstance.create(bearDef, player1Id.value)
            val blocker = CardInstance.create(bearDef, player2Id.value)
            val otherAttacker = CardInstance.create(bearDef, player1Id.value)

            val decision = ChooseBlockers(
                playerId = player2Id,
                legalBlockers = listOf(blocker.id),
                attackers = listOf(attacker.id),
                legalBlocks = mapOf(blocker.id to listOf(attacker.id)) // Can only block attacker
            )

            val response = BlockersChoice(mapOf(blocker.id to otherAttacker.id)) // Try to block other
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val result = DecisionValidator.validate(state, decision, response)

            result.shouldBeInstanceOf<DecisionValidator.ValidationResult.Invalid>()
        }
    }

    context("validateManaPayment") {
        test("valid mana payment is accepted") {
            val decision = ChooseManaPayment(
                playerId = player1Id,
                cardName = "Test Spell",
                requiredWhite = 1,
                requiredGeneric = 2,
                availableMana = mapOf(Color.WHITE to 3, Color.GREEN to 2),
                availableColorless = 0
            )

            // Pay 1W for white requirement, 2W for generic
            val response = ManaPaymentChoice(whiteForGeneric = 2)
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val result = DecisionValidator.validate(state, decision, response)

            result.shouldBeInstanceOf<DecisionValidator.ValidationResult.Valid>()
        }

        test("insufficient mana is rejected") {
            val decision = ChooseManaPayment(
                playerId = player1Id,
                cardName = "Test Spell",
                requiredWhite = 1,
                requiredGeneric = 2,
                availableMana = mapOf(Color.WHITE to 2), // Only 2 white
                availableColorless = 0
            )

            // Try to pay 2W for generic (need 1W + 2W = 3W, only have 2)
            val response = ManaPaymentChoice(whiteForGeneric = 2)
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val result = DecisionValidator.validate(state, decision, response)

            result.shouldBeInstanceOf<DecisionValidator.ValidationResult.Invalid>()
        }

        test("wrong generic total is rejected") {
            val decision = ChooseManaPayment(
                playerId = player1Id,
                cardName = "Test Spell",
                requiredGeneric = 3,
                availableMana = mapOf(Color.WHITE to 5),
                availableColorless = 0
            )

            // Only pay 2 for generic when 3 is required
            val response = ManaPaymentChoice(whiteForGeneric = 2)
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val result = DecisionValidator.validate(state, decision, response)

            result.shouldBeInstanceOf<DecisionValidator.ValidationResult.Invalid>()
        }
    }

    context("validateCardsChoice") {
        test("valid card selection is accepted") {
            val card1 = CardId("card1")
            val card2 = CardId("card2")
            val card3 = CardId("card3")

            val decision = ChooseCards(
                playerId = player1Id,
                description = "Discard 2 cards",
                cards = listOf(card1, card2, card3),
                minCount = 2,
                maxCount = 2
            )

            val response = CardsChoice(listOf(card1, card2))
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val result = DecisionValidator.validate(state, decision, response)

            result.shouldBeInstanceOf<DecisionValidator.ValidationResult.Valid>()
        }

        test("too few cards is rejected") {
            val card1 = CardId("card1")
            val card2 = CardId("card2")

            val decision = ChooseCards(
                playerId = player1Id,
                description = "Discard 2 cards",
                cards = listOf(card1, card2),
                minCount = 2,
                maxCount = 2
            )

            val response = CardsChoice(listOf(card1))
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val result = DecisionValidator.validate(state, decision, response)

            result.shouldBeInstanceOf<DecisionValidator.ValidationResult.Invalid>()
        }

        test("too many cards is rejected") {
            val card1 = CardId("card1")
            val card2 = CardId("card2")
            val card3 = CardId("card3")

            val decision = ChooseCards(
                playerId = player1Id,
                description = "Discard up to 2 cards",
                cards = listOf(card1, card2, card3),
                minCount = 0,
                maxCount = 2
            )

            val response = CardsChoice(listOf(card1, card2, card3))
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val result = DecisionValidator.validate(state, decision, response)

            result.shouldBeInstanceOf<DecisionValidator.ValidationResult.Invalid>()
        }

        test("invalid card is rejected") {
            val card1 = CardId("card1")
            val card2 = CardId("card2")
            val invalidCard = CardId("invalid")

            val decision = ChooseCards(
                playerId = player1Id,
                description = "Choose a card",
                cards = listOf(card1, card2),
                minCount = 1,
                maxCount = 1
            )

            val response = CardsChoice(listOf(invalidCard))
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val result = DecisionValidator.validate(state, decision, response)

            result.shouldBeInstanceOf<DecisionValidator.ValidationResult.Invalid>()
        }
    }

    context("validateNumberChoice") {
        test("valid number is accepted") {
            val decision = ChooseNumber(
                playerId = player1Id,
                description = "Choose X",
                minimum = 0,
                maximum = 10
            )

            val response = NumberChoice(5)
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val result = DecisionValidator.validate(state, decision, response)

            result.shouldBeInstanceOf<DecisionValidator.ValidationResult.Valid>()
        }

        test("number below minimum is rejected") {
            val decision = ChooseNumber(
                playerId = player1Id,
                description = "Choose X",
                minimum = 3,
                maximum = 10
            )

            val response = NumberChoice(2)
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val result = DecisionValidator.validate(state, decision, response)

            result.shouldBeInstanceOf<DecisionValidator.ValidationResult.Invalid>()
        }

        test("number above maximum is rejected") {
            val decision = ChooseNumber(
                playerId = player1Id,
                description = "Choose X",
                minimum = 0,
                maximum = 5
            )

            val response = NumberChoice(7)
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val result = DecisionValidator.validate(state, decision, response)

            result.shouldBeInstanceOf<DecisionValidator.ValidationResult.Invalid>()
        }
    }

    context("validateModeChoice") {
        test("valid mode selection is accepted") {
            val decision = ChooseMode(
                playerId = player1Id,
                sourceCardId = CardId("charm"),
                sourceName = "Test Charm",
                modes = listOf(
                    ModeOption(0, "Mode 1"),
                    ModeOption(1, "Mode 2"),
                    ModeOption(2, "Mode 3")
                ),
                minModes = 1,
                maxModes = 2
            )

            val response = ModeChoice(listOf(0, 2))
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val result = DecisionValidator.validate(state, decision, response)

            result.shouldBeInstanceOf<DecisionValidator.ValidationResult.Valid>()
        }

        test("unavailable mode is rejected") {
            val decision = ChooseMode(
                playerId = player1Id,
                sourceCardId = CardId("charm"),
                sourceName = "Test Charm",
                modes = listOf(
                    ModeOption(0, "Mode 1", isAvailable = true),
                    ModeOption(1, "Mode 2", isAvailable = false) // Unavailable
                ),
                minModes = 1,
                maxModes = 1
            )

            val response = ModeChoice(listOf(1)) // Try to choose unavailable mode
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val result = DecisionValidator.validate(state, decision, response)

            result.shouldBeInstanceOf<DecisionValidator.ValidationResult.Invalid>()
        }

        test("repeated modes without permission is rejected") {
            val decision = ChooseMode(
                playerId = player1Id,
                sourceCardId = CardId("charm"),
                sourceName = "Test Charm",
                modes = listOf(
                    ModeOption(0, "Mode 1"),
                    ModeOption(1, "Mode 2")
                ),
                minModes = 2,
                maxModes = 2,
                canRepeatModes = false
            )

            val response = ModeChoice(listOf(0, 0)) // Try to repeat mode
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val result = DecisionValidator.validate(state, decision, response)

            result.shouldBeInstanceOf<DecisionValidator.ValidationResult.Invalid>()
        }
    }

    context("validateMulliganBottomCards") {
        test("valid mulligan bottom cards is accepted") {
            val card1 = CardId("card1")
            val card2 = CardId("card2")
            val card3 = CardId("card3")

            val decision = ChooseMulliganBottomCards(
                playerId = player1Id,
                hand = listOf(card1, card2, card3),
                cardsToPutOnBottom = 1
            )

            val response = CardsChoice(listOf(card2))
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val result = DecisionValidator.validate(state, decision, response)

            result.shouldBeInstanceOf<DecisionValidator.ValidationResult.Valid>()
        }

        test("wrong number of cards is rejected") {
            val card1 = CardId("card1")
            val card2 = CardId("card2")
            val card3 = CardId("card3")

            val decision = ChooseMulliganBottomCards(
                playerId = player1Id,
                hand = listOf(card1, card2, card3),
                cardsToPutOnBottom = 2
            )

            val response = CardsChoice(listOf(card1)) // Only 1 card instead of 2
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val result = DecisionValidator.validate(state, decision, response)

            result.shouldBeInstanceOf<DecisionValidator.ValidationResult.Invalid>()
        }
    }

    context("validatePriorityChoice") {
        test("pass is always valid") {
            val decision = PriorityDecision(
                playerId = player1Id,
                canCastSpells = false,
                canActivateAbilities = false,
                canPlayLand = false,
                stackIsEmpty = true
            )

            val response = PriorityChoice.Pass
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val result = DecisionValidator.validate(state, decision, response)

            result.shouldBeInstanceOf<DecisionValidator.ValidationResult.Valid>()
        }

        test("play land when not allowed is rejected") {
            val decision = PriorityDecision(
                playerId = player1Id,
                canCastSpells = true,
                canActivateAbilities = true,
                canPlayLand = false, // Cannot play land
                stackIsEmpty = true
            )

            val response = PriorityChoice.PlayLand(CardId("forest"))
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val result = DecisionValidator.validate(state, decision, response)

            result.shouldBeInstanceOf<DecisionValidator.ValidationResult.Invalid>()
        }
    }
})
