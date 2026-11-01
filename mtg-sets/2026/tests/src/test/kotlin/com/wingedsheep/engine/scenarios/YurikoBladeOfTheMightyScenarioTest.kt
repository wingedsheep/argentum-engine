package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CycleCard
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.fra.cards.SeasonedCryomancer
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Yuriko, Blade of the Mighty (FRA #210) — during combat nobody can cast spells or activate
 * non-mana abilities (from any zone); a creature you control attacking a player alone gains
 * double strike until end of turn.
 */
class YurikoBladeOfTheMightyScenarioTest : ScenarioTestBase() {

    init {
        fun game(phase: Phase, step: Step, configure: (ScenarioBuilder) -> ScenarioBuilder = { it }): TestGame {
            var b = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Yuriko, Blade of the Mighty")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(phase, step)
            b = configure(b)
            repeat(4) { b = b.withCardInLibrary(1, "Island") }
            repeat(4) { b = b.withCardInLibrary(2, "Island") }
            return b.build()
        }

        context("attacks a player alone") {
            test("the lone attacker gains double strike") {
                val game = game(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.hasKeyword(bears, Keyword.DOUBLE_STRIKE) shouldBe true
            }

            test("two attackers — neither is alone") {
                val game = game(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2, "Yuriko, Blade of the Mighty" to 2)).error shouldBe null
                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.hasKeyword(bears, Keyword.DOUBLE_STRIKE) shouldBe false
            }

            // CR 506.6: alone against *that player* — a second attacker on a planeswalker doesn't count.
            test("the player's only attacker gains double strike while another attacks a planeswalker") {
                val game = game(Phase.COMBAT, Step.DECLARE_ATTACKERS) { it.withCardOnBattlefield(2, "Liliana Vess") }
                game.declareAttackersWithPermanentTargets(
                    playerAttackers = mapOf("Grizzly Bears" to 2),
                    permanentAttackers = mapOf("Yuriko, Blade of the Mighty" to "Liliana Vess")
                ).error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                projected.hasKeyword(game.findPermanent("Grizzly Bears")!!, Keyword.DOUBLE_STRIKE) shouldBe true
                projected.hasKeyword(game.findPermanent("Yuriko, Blade of the Mighty")!!, Keyword.DOUBLE_STRIKE) shouldBe false
            }

            test("attacking a planeswalker alone isn't attacking a player alone") {
                val game = game(Phase.COMBAT, Step.DECLARE_ATTACKERS) { it.withCardOnBattlefield(2, "Liliana Vess") }
                game.declareAttackersWithPermanentTargets(
                    permanentAttackers = mapOf("Grizzly Bears" to "Liliana Vess")
                ).error shouldBe null
                game.resolveStack()

                game.state.projectedState.hasKeyword(game.findPermanent("Grizzly Bears")!!, Keyword.DOUBLE_STRIKE) shouldBe false
            }
        }

        context("during combat, players can't cast spells or activate non-mana abilities") {
            test("spells can't be cast during combat, by either player") {
                val game = game(Phase.COMBAT, Step.BEGIN_COMBAT) {
                    it.withCardInHand(1, "Giant Growth").withLandsOnBattlefield(1, "Forest", 1)
                }
                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("the controller is a player too") {
                    game.castSpell(1, "Giant Growth", bears).error shouldNotBe null
                }
            }

            test("spells can be cast outside combat") {
                val game = game(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN) {
                    it.withCardInHand(1, "Giant Growth").withLandsOnBattlefield(1, "Forest", 1)
                }
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Giant Growth", bears).error shouldBe null
            }

            test("a graveyard ability can't be activated during combat, and isn't offered") {
                val game = game(Phase.COMBAT, Step.BEGIN_COMBAT) {
                    it.withCardInGraveyard(1, "Seasoned Cryomancer").withLandsOnBattlefield(1, "Island", 5)
                }
                val card = game.findCardsInGraveyard(1, "Seasoned Cryomancer").single()
                val abilityId = SeasonedCryomancer.activatedAbilities.single().id

                game.getLegalActions(1).any { (it.action as? ActivateAbility)?.sourceId == card } shouldBe false
                game.execute(ActivateAbility(game.player1Id, card, abilityId)).error shouldNotBe null
                withClue("mana abilities stay available") {
                    val islands = game.findPermanents("Island").toSet()
                    game.getLegalActions(1).any { (it.action as? ActivateAbility)?.sourceId in islands } shouldBe true
                }
            }

            test("the same graveyard ability works outside combat") {
                val game = game(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN) {
                    it.withCardInGraveyard(1, "Seasoned Cryomancer").withLandsOnBattlefield(1, "Island", 5)
                }
                val card = game.findCardsInGraveyard(1, "Seasoned Cryomancer").single()
                val abilityId = SeasonedCryomancer.activatedAbilities.single().id
                game.execute(ActivateAbility(game.player1Id, card, abilityId)).error shouldBe null
            }

            test("cycling is an activated ability, so it's forbidden during combat") {
                val game = game(Phase.COMBAT, Step.BEGIN_COMBAT) {
                    it.withCardInHand(1, "Disciple of Law").withLandsOnBattlefield(1, "Plains", 2)
                }
                game.getLegalActions(1).any { it.action is CycleCard } shouldBe false
                game.cycleCard(1, "Disciple of Law").error shouldNotBe null
            }
        }
    }
}
