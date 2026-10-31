package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.PlayerHexproofFromComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.war.cards.KayaBaneOfTheDead
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ProtectionScope
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Kaya, Bane of the Dead (WAR #231).
 *
 *   Your opponents and permanents your opponents control with hexproof can be the targets of
 *   spells and abilities you control as though they didn't have hexproof.
 *   −3: Exile target creature.
 *
 * Covers both halves of the static (players via Leyline of Sanctity, permanents via Geist of Saint
 * Traft), that it only helps Kaya's controller, that it reaches "hexproof from" (CR 702.11e), and
 * the ruling that a target becomes illegal once Kaya leaves before the spell resolves.
 */
class KayaBaneOfTheDeadScenarioTest : ScenarioTestBase() {

    private fun shockTargetsOpponent(game: TestGame): Boolean =
        game.getLegalActions(1).any { action ->
            action.description.contains("Shock") &&
                action.validTargets.orEmpty().contains(game.player2Id)
        }

    init {
        test("you can target an opponent with hexproof") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Kaya, Bane of the Dead")
                .withCardOnBattlefield(2, "Leyline of Sanctity")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Shock")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            withClue("Shock offers the hexproof opponent as a target") { shockTargetsOpponent(game) shouldBe true }
            game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 18
        }

        test("without Kaya the hexproof opponent can't be targeted") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(2, "Leyline of Sanctity")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Shock")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            shockTargetsOpponent(game) shouldBe false
            game.castSpellTargetingPlayer(1, "Shock", 2).error shouldNotBe null
        }

        test("the opponent's Kaya doesn't let them through your hexproof, and yours doesn't help them") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(2, "Kaya, Bane of the Dead")
                .withCardOnBattlefield(2, "Leyline of Sanctity")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Shock")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            withClue("an opponent's Kaya grants you nothing") { shockTargetsOpponent(game) shouldBe false }
        }

        test("it reaches hexproof from a quality too") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Kaya, Bane of the Dead")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Shock")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.state = game.state.updateEntity(game.player2Id) { c ->
                c.with(PlayerHexproofFromComponent(listOf(PlayerHexproofFromComponent.Grant(ProtectionScope.Color(Color.RED)))))
            }

            shockTargetsOpponent(game) shouldBe true
            game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 18
        }

        test("−3 exiles an opponent's hexproof creature") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Kaya, Bane of the Dead")
                .withCardOnBattlefield(2, "Geist of Saint Traft")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val kaya = game.findPermanent("Kaya, Bane of the Dead")!!
            val geist = game.findPermanent("Geist of Saint Traft")!!
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = kaya,
                    abilityId = KayaBaneOfTheDead.activatedAbilities[0].id,
                    targets = listOf(ChosenTarget.Permanent(geist)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Geist of Saint Traft") shouldBe true
        }

        test("−3 is offered against a creature with hexproof from black, even though Kaya is black") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Kaya, Bane of the Dead")
                .withCardOnBattlefield(2, "Knight of Grace")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val knight = game.findPermanent("Knight of Grace")!!
            val minusThree = game.getLegalActions(1).single { action ->
                (action.action as? ActivateAbility)?.abilityId == KayaBaneOfTheDead.activatedAbilities[0].id
            }
            minusThree.validTargets.orEmpty().contains(knight) shouldBe true
        }

        test("if Kaya leaves before the spell resolves, the hexproof opponent is an illegal target") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Kaya, Bane of the Dead")
                .withCardOnBattlefield(2, "Leyline of Sanctity")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withLandsOnBattlefield(2, "Swamp", 3)
                .withCardInHand(1, "Shock")
                .withCardInHand(2, "Hero's Downfall")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val kaya = game.findPermanent("Kaya, Bane of the Dead")!!
            game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
            game.passPriority().error shouldBe null
            game.castSpell(2, "Hero's Downfall", kaya).error shouldBe null
            game.resolveStack()

            withClue("Kaya was destroyed in response") { game.isInGraveyard(1, "Kaya, Bane of the Dead") shouldBe true }
            withClue("Shock lost its only target and did nothing") { game.getLifeTotal(2) shouldBe 20 }
        }
    }
}
