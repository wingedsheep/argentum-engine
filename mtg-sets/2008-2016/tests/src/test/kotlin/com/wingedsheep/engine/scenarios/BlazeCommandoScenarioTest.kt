package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Blaze Commando (DGM #56) — "Whenever an instant or sorcery spell you control
 * deals damage, create two 1/1 red and white Soldier creature tokens with haste."
 *
 * Per the 2013-04-15 ruling it triggers once per "deals" in the spell's instructions, however many
 * players or permanents that instruction damages.
 */
class BlazeCommandoScenarioTest : ScenarioTestBase() {

    init {
        // Two separate damage instructions in one resolution: "deals 1 damage to each opponent.
        // You gain 1 life. This spell deals 1 damage to each opponent."
        cardRegistry.register(
            card("Test Double Singe") {
                manaCost = "{R}"
                typeLine = "Sorcery"
                oracleText = "Test Double Singe deals 1 damage to each opponent. You gain 1 life. " +
                    "Test Double Singe deals 1 damage to each opponent."
                spell {
                    effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent)) then
                        Effects.GainLife(1) then
                        Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
                }
            }
        )

        fun board(vararg hand: String, opponentCreatures: List<String> = emptyList()): TestGame {
            val builder = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Blaze Commando")
                .withLandsOnBattlefield(1, "Mountain", 6)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            hand.forEach { builder.withCardInHand(1, it) }
            opponentCreatures.forEach { builder.withCardOnBattlefield(2, it) }
            return builder.build()
        }

        fun TestGame.soldiers() = findPermanents("Soldier Token")

        context("Blaze Commando") {

            test("a burn spell's damage makes two hasty red and white Soldiers") {
                val game = board("Lightning Bolt")
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 17
                val soldiers = game.soldiers()
                soldiers.size shouldBe 2
                soldiers.forEach { id ->
                    game.state.projectedState.hasKeyword(id, Keyword.HASTE) shouldBe true
                    game.state.projectedState.getPower(id) shouldBe 1
                }
            }

            test("one \"deals\" to a creature and its controller triggers once") {
                val game = board("Chandra's Outrage", opponentCreatures = listOf("Hill Giant"))
                game.castSpell(1, "Chandra's Outrage", game.findPermanent("Hill Giant")!!).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(2, "Hill Giant") shouldBe true
                game.getLifeTotal(2) shouldBe 18
                withClue("4 to the creature and 2 to its controller is one instruction") { game.soldiers().size shouldBe 2 }
            }

            test("one instruction damaging several creatures triggers once") {
                val game = board("Electrickery", opponentCreatures = listOf("Llanowar Elves", "Fyndhorn Elves"))
                game.castSpellWithOverload(1, "Electrickery").error shouldBe null
                game.resolveStack()
                game.isInGraveyard(2, "Llanowar Elves") shouldBe true
                game.isInGraveyard(2, "Fyndhorn Elves") shouldBe true
                game.soldiers().size shouldBe 2
            }

            test("two separate damage instructions in one spell trigger twice") {
                val game = board("Test Double Singe")
                game.castSpell(1, "Test Double Singe").error shouldBe null
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 18
                game.soldiers().size shouldBe 4
            }

            test("an opponent's spell and creature combat damage don't trigger it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Blaze Commando")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 17
                game.soldiers().size shouldBe 0
            }

            test("its own combat damage doesn't trigger it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Blaze Commando", summoningSickness = false)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()
                game.declareAttackers(mapOf("Blaze Commando" to 2)).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.getLifeTotal(2) shouldBe 15
                game.soldiers().size shouldBe 0
            }
        }
    }
}
