package com.wingedsheep.rulesengine.casting

import com.wingedsheep.rulesengine.action.*
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.game.GameState
import com.wingedsheep.rulesengine.game.Phase
import com.wingedsheep.rulesengine.game.Step
import com.wingedsheep.rulesengine.player.Player
import com.wingedsheep.rulesengine.player.PlayerId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class SpellCastingTest : FunSpec({

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

    val flashCreatureDef = CardDefinition.creature(
        name = "Ambush Viper",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype(value = "Snake")),
        power = 2,
        toughness = 1,
        keywords = setOf(Keyword.FLASH, Keyword.DEATHTOUCH)
    )

    fun createGameInMainPhase(): GameState {
        val state = GameState.newGame(createPlayer1(), createPlayer2())
        return state.advanceToStep(Step.PRECOMBAT_MAIN)
    }

    context("SpellTimingValidator") {
        context("sorcery speed") {
            test("can cast sorcery during main phase with empty stack as active player") {
                val state = createGameInMainPhase()
                val sorcery = CardInstance.create(lavaAxeDef, player1Id.value)

                val result = SpellTimingValidator.canCast(state, sorcery, player1Id)

                result.shouldBeInstanceOf<SpellTimingValidator.TimingResult.Valid>()
            }

            test("cannot cast sorcery during combat phase") {
                val state = createGameInMainPhase().advanceToPhase(Phase.COMBAT)
                val sorcery = CardInstance.create(lavaAxeDef, player1Id.value)

                val result = SpellTimingValidator.canCast(state, sorcery, player1Id)

                result.shouldBeInstanceOf<SpellTimingValidator.TimingResult.Invalid>()
                (result as SpellTimingValidator.TimingResult.Invalid).reason shouldBe
                    "Sorcery-speed spells can only be cast during main phase"
            }

            test("cannot cast sorcery when stack is not empty") {
                var state = createGameInMainPhase()
                val instant = CardInstance.create(lightningBoltDef, player1Id.value)
                state = state.updateStack { it.addToTop(instant) }

                val sorcery = CardInstance.create(lavaAxeDef, player1Id.value)
                val result = SpellTimingValidator.canCast(state, sorcery, player1Id)

                result.shouldBeInstanceOf<SpellTimingValidator.TimingResult.Invalid>()
                (result as SpellTimingValidator.TimingResult.Invalid).reason shouldBe
                    "Sorcery-speed spells can only be cast when the stack is empty"
            }

            test("cannot cast sorcery when not active player") {
                val state = createGameInMainPhase()
                val sorcery = CardInstance.create(lavaAxeDef, player2Id.value)

                // Player 2 has priority but is not active player
                val stateWithP2Priority = state.updateTurnState { it.copy(priorityPlayer = player2Id) }

                val result = SpellTimingValidator.canCast(stateWithP2Priority, sorcery, player2Id)

                result.shouldBeInstanceOf<SpellTimingValidator.TimingResult.Invalid>()
                (result as SpellTimingValidator.TimingResult.Invalid).reason shouldBe
                    "Only the active player can cast sorcery-speed spells"
            }

            test("creatures are sorcery speed") {
                val state = createGameInMainPhase()
                val creature = CardInstance.create(bearDef, player1Id.value)

                val result = SpellTimingValidator.canCast(state, creature, player1Id)

                result.shouldBeInstanceOf<SpellTimingValidator.TimingResult.Valid>()
            }
        }

        context("instant speed") {
            test("can cast instant during main phase") {
                val state = createGameInMainPhase()
                val instant = CardInstance.create(lightningBoltDef, player1Id.value)

                val result = SpellTimingValidator.canCast(state, instant, player1Id)

                result.shouldBeInstanceOf<SpellTimingValidator.TimingResult.Valid>()
            }

            test("can cast instant during combat phase") {
                val state = createGameInMainPhase()
                    .advanceToPhase(Phase.COMBAT)
                    .advanceToStep(Step.DECLARE_ATTACKERS)
                val instant = CardInstance.create(lightningBoltDef, player1Id.value)

                val result = SpellTimingValidator.canCast(state, instant, player1Id)

                result.shouldBeInstanceOf<SpellTimingValidator.TimingResult.Valid>()
            }

            test("can cast instant when stack is not empty") {
                var state = createGameInMainPhase()
                val instant1 = CardInstance.create(lightningBoltDef, player1Id.value)
                state = state.updateStack { it.addToTop(instant1) }

                val instant2 = CardInstance.create(lightningBoltDef, player1Id.value)
                val result = SpellTimingValidator.canCast(state, instant2, player1Id)

                result.shouldBeInstanceOf<SpellTimingValidator.TimingResult.Valid>()
            }

            test("can cast instant as non-active player with priority") {
                val state = createGameInMainPhase()
                    .updateTurnState { it.copy(priorityPlayer = player2Id) }
                val instant = CardInstance.create(lightningBoltDef, player2Id.value)

                val result = SpellTimingValidator.canCast(state, instant, player2Id)

                result.shouldBeInstanceOf<SpellTimingValidator.TimingResult.Valid>()
            }

            test("cannot cast instant without priority") {
                val state = createGameInMainPhase()
                val instant = CardInstance.create(lightningBoltDef, player2Id.value)

                val result = SpellTimingValidator.canCast(state, instant, player2Id)

                result.shouldBeInstanceOf<SpellTimingValidator.TimingResult.Invalid>()
                (result as SpellTimingValidator.TimingResult.Invalid).reason shouldBe
                    "You don't have priority"
            }

            test("cannot cast instant during untap step") {
                val state = GameState.newGame(createPlayer1(), createPlayer2())
                // Game starts in untap step, which has no priority
                val instant = CardInstance.create(lightningBoltDef, player1Id.value)

                val result = SpellTimingValidator.canCast(state, instant, player1Id)

                result.shouldBeInstanceOf<SpellTimingValidator.TimingResult.Invalid>()
            }
        }

        context("flash") {
            test("creature with flash can be cast at instant speed") {
                val state = createGameInMainPhase()
                    .advanceToPhase(Phase.COMBAT)
                    .advanceToStep(Step.DECLARE_BLOCKERS)
                val flashCreature = CardInstance.create(flashCreatureDef, player1Id.value)

                val result = SpellTimingValidator.canCast(state, flashCreature, player1Id)

                result.shouldBeInstanceOf<SpellTimingValidator.TimingResult.Valid>()
            }

            test("creature with flash can be cast with non-empty stack") {
                var state = createGameInMainPhase()
                val instant = CardInstance.create(lightningBoltDef, player1Id.value)
                state = state.updateStack { it.addToTop(instant) }

                val flashCreature = CardInstance.create(flashCreatureDef, player1Id.value)
                val result = SpellTimingValidator.canCast(state, flashCreature, player1Id)

                result.shouldBeInstanceOf<SpellTimingValidator.TimingResult.Valid>()
            }
        }

        context("game state checks") {
            test("cannot cast spell when game is over") {
                val state = createGameInMainPhase().endGame(player1Id)
                val instant = CardInstance.create(lightningBoltDef, player1Id.value)

                val result = SpellTimingValidator.canCast(state, instant, player1Id)

                result.shouldBeInstanceOf<SpellTimingValidator.TimingResult.Invalid>()
                (result as SpellTimingValidator.TimingResult.Invalid).reason shouldBe "Game is over"
            }
        }

        context("isValidTiming convenience method") {
            test("returns true for valid timing") {
                val state = createGameInMainPhase()
                val sorcery = CardInstance.create(lavaAxeDef, player1Id.value)

                SpellTimingValidator.isValidTiming(state, sorcery, player1Id).shouldBeTrue()
            }

            test("returns false for invalid timing") {
                val state = createGameInMainPhase().advanceToPhase(Phase.COMBAT)
                val sorcery = CardInstance.create(lavaAxeDef, player1Id.value)

                SpellTimingValidator.isValidTiming(state, sorcery, player1Id).shouldBeFalse()
            }
        }
    }

    context("ManaPaymentValidator") {
        test("can pay when player has enough mana") {
            var state = createGameInMainPhase()
            state = state.updatePlayer(player1Id) { p ->
                p.addMana(Color.GREEN, 1).addMana(Color.RED, 1).addColorlessMana(3)
            }

            val sorcery = CardInstance.create(lavaAxeDef, player1Id.value)
            val result = ManaPaymentValidator.canPay(state, sorcery, player1Id)

            result.shouldBeInstanceOf<ManaPaymentValidator.PaymentResult.Valid>()
        }

        test("cannot pay when player has insufficient mana") {
            val state = createGameInMainPhase()
            val sorcery = CardInstance.create(lavaAxeDef, player1Id.value)

            val result = ManaPaymentValidator.canPay(state, sorcery, player1Id)

            result.shouldBeInstanceOf<ManaPaymentValidator.PaymentResult.Invalid>()
        }

        test("cannot pay when missing colored mana") {
            var state = createGameInMainPhase()
            // Has enough total mana but not the required red
            state = state.updatePlayer(player1Id) { p ->
                p.addMana(Color.GREEN, 5)
            }

            val sorcery = CardInstance.create(lavaAxeDef, player1Id.value)
            val result = ManaPaymentValidator.canPay(state, sorcery, player1Id)

            result.shouldBeInstanceOf<ManaPaymentValidator.PaymentResult.Invalid>()
        }

        test("isValidPayment convenience method") {
            var state = createGameInMainPhase()
            state = state.updatePlayer(player1Id) { p -> p.addMana(Color.RED, 1) }

            val instant = CardInstance.create(lightningBoltDef, player1Id.value)

            ManaPaymentValidator.isValidPayment(state, instant, player1Id).shouldBeTrue()
        }
    }
})
