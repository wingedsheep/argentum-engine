package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The Siege defeat trigger (CR 310.11b): "When the last defense counter is removed from this
 * permanent, exile it, then you may cast it transformed without paying its mana cost."
 *
 * Every rule and Gatherer ruling that governs it gets an assertion here, because none of it is
 * printed on any card — the ability is synthesized by the engine for any Siege on the battlefield,
 * so a card-level test could never catch a regression in the ability itself.
 *
 * Two inline test Sieges: a transforming one, and one with no back face (the "a card that isn't
 * represented by a transforming double-faced card can't be cast; it remains in exile" ruling).
 * Defense is chipped with Lightning Bolt, which is both the shortest path through the full
 * cast → resolve → trigger-detection pipeline and a check that CR 115.4's "any target" reaches a
 * battle at all.
 */
class SiegeDefeatTriggerScenarioTest : ScenarioTestBase() {

    private val testSiegeBack = card("Test Siege Aftermath") {
        manaCost = ""
        colorIdentity = "B"
        colorIndicator = "B"
        typeLine = "Enchantment"
        oracleText = "When this enchantment enters, you gain 3 life."

        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            effect = Effects.GainLife(3)
            description = "When this enchantment enters, you gain 3 life."
        }
    }

    private val testSiegeFront = card("Test Siege") {
        manaCost = "{2}{B}{B}"
        colorIdentity = "B"
        typeLine = "Battle — Siege"
        startingDefense = 6
        oracleText = "(As a Siege enters, choose an opponent to protect it.)"
    }

    private val testSiege: CardDefinition = CardDefinition.doubleFacedPermanent(
        frontFace = testSiegeFront,
        backFace = testSiegeBack,
    )

    /** A Siege with no back face — nothing to cast transformed, so it just stays exiled. */
    private val testOneSidedSiege = card("Test Blockade") {
        manaCost = "{1}{W}"
        colorIdentity = "W"
        typeLine = "Battle — Siege"
        startingDefense = 3
        oracleText = "A Siege with no back face."
    }

    private fun defenseOf(game: TestGame, name: String): Int =
        game.findPermanent(name)
            ?.let { game.state.getEntity(it)?.get<CountersComponent>()?.getCount(CounterType.DEFENSE) }
            ?: 0

    /**
     * A board with [siegeName] on the battlefield (protector already designated by the SBA) and
     * [bolts] Lightning Bolts in hand with the mana to cast them.
     */
    private fun siegeInPlay(siegeName: String, bolts: Int = 2): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, siegeName)
            .withLandsOnBattlefield(1, "Mountain", bolts)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(bolts) { builder.withCardInHand(1, "Lightning Bolt") }
        val game = builder.build()
        game.checkStateBasedActions()
        return game
    }

    /** Bolt [siegeName] for 3, resolving the bolt but leaving any resulting trigger on the stack. */
    private fun bolt(game: TestGame, siegeName: String) {
        val siegeId = game.findPermanent(siegeName) ?: error("$siegeName is not on the battlefield")
        game.castSpell(1, "Lightning Bolt", siegeId).error shouldBe null
        game.resolveStack()
    }

    init {
        cardRegistry.register(testSiege)
        cardRegistry.register(testOneSidedSiege)

        context("CR 115.4 — a battle can be targeted by 'any target'") {

            test("a battle is offered as a legal target for a damage spell") {
                val game = siegeInPlay("Test Siege")
                val siegeId = game.findPermanent("Test Siege")!!

                withClue("CR 115.4 — 'any target' means a creature, player, planeswalker, or battle") {
                    game.castSpell(1, "Lightning Bolt", siegeId).error shouldBe null
                }
                game.resolveStack()

                withClue("CR 120.3h — damage to a battle removes that many defense counters") {
                    defenseOf(game, "Test Siege") shouldBe 3
                }
            }
        }

        context("CR 310.11b — when the last defense counter is removed") {

            test("chipping a Siege without emptying it doesn't trigger anything") {
                val game = siegeInPlay("Test Siege")

                bolt(game, "Test Siege")

                withClue("6 defense minus 3 damage leaves 3 counters") {
                    defenseOf(game, "Test Siege") shouldBe 3
                }
                withClue("the defeat trigger fires only for the removal that empties the pile") {
                    game.state.stack.isEmpty() shouldBe true
                    game.state.pendingDecision shouldBe null
                    game.isOnBattlefield("Test Siege") shouldBe true
                }
            }

            test("removing the last defense counter exiles the Siege and offers a transformed free cast") {
                val game = siegeInPlay("Test Siege")

                bolt(game, "Test Siege")
                bolt(game, "Test Siege")

                withClue("'exile it' is mandatory — the battle leaves the battlefield either way") {
                    game.isOnBattlefield("Test Siege") shouldBe false
                }
                withClue("CR 704.5v's carve-out kept the battle alive for its own trigger: exiled, not binned") {
                    game.isInGraveyard(1, "Test Siege") shouldBe false
                }
                withClue("'then you may cast it transformed' is offered to the battle's controller") {
                    game.state.pendingDecision shouldNotBe null
                    game.state.pendingDecision!!.playerId shouldBe game.player1Id
                }

                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                withClue("the cast puts the BACK face on the stack (CR 712.8c), so the back face resolves") {
                    game.isOnBattlefield("Test Siege Aftermath") shouldBe true
                    game.isOnBattlefield("Test Siege") shouldBe false
                }
                withClue("and the back face's own enters trigger fires: 20 + 3") {
                    game.resolveStack()
                    game.getLifeTotal(1) shouldBe 23
                }
            }

            test("declining the cast leaves the card in exile") {
                val game = siegeInPlay("Test Siege")

                bolt(game, "Test Siege")
                bolt(game, "Test Siege")
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                withClue("a card left uncast simply stays exiled — it never reaches the graveyard") {
                    game.isInExile(1, "Test Siege") shouldBe true
                    game.isInGraveyard(1, "Test Siege") shouldBe false
                    game.isOnBattlefield("Test Siege Aftermath") shouldBe false
                }
            }

            test("a Siege with no back face is exiled and stays there") {
                val game = siegeInPlay("Test Blockade", bolts = 1)

                bolt(game, "Test Blockade")
                if (game.state.pendingDecision != null) game.answerYesNo(true)
                game.resolveStack()

                withClue(
                    "ruling: a Siege not represented by a transforming double-faced card can't be " +
                        "cast as its trigger resolves; it remains in exile"
                ) {
                    game.isInExile(1, "Test Blockade") shouldBe true
                    game.isOnBattlefield("Test Blockade") shouldBe false
                    game.isInGraveyard(1, "Test Blockade") shouldBe false
                }
            }

            test("a Siege that never had defense counters is binned without triggering") {
                val game = siegeInPlay("Test Siege", bolts = 0)
                val siegeId = game.findPermanent("Test Siege")!!

                // Strip the counters without emitting a removal event — the shape the ruling
                // describes, where a permanent becomes a copy of a Siege and so never had any.
                game.state = game.state.updateEntity(siegeId) { container ->
                    container.with(CountersComponent())
                }
                game.checkStateBasedActions().error shouldBe null

                withClue(
                    "ruling: if a Siege never had defense counters, it can't have its last one " +
                        "removed — CR 704.5v bins it and the defeat trigger never fires"
                ) {
                    game.isInGraveyard(1, "Test Siege") shouldBe true
                    game.isInExile(1, "Test Siege") shouldBe false
                    game.state.stack.isEmpty() shouldBe true
                    game.state.pendingDecision shouldBe null
                }
            }
        }

        context("CR 310.6 — combat damage is the ordinary way a Siege is defeated") {

            test("combat damage that removes the last defense counter defeats the Siege") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Test Blockade")
                    .withCardOnBattlefield(1, "Serra Angel", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.checkStateBasedActions()
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackersWithPermanentTargets(
                    permanentAttackers = mapOf("Serra Angel" to "Test Blockade")
                ).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("a 4/4 removes all 3 defense counters and defeats the Siege") {
                    game.isOnBattlefield("Test Blockade") shouldBe false
                }
                withClue("the defeat trigger exiled it rather than the SBA binning it") {
                    game.isInExile(1, "Test Blockade") shouldBe true
                    game.isInGraveyard(1, "Test Blockade") shouldBe false
                }
            }
        }
    }
}
