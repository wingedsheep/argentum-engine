package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sword of Forge and Frontier — {3} Artifact — Equipment:
 *   "Equipped creature gets +2/+2 and has protection from red and from green.
 *    Whenever equipped creature deals combat damage to a player, exile the top two cards of your
 *    library. You may play those cards this turn. You may play an additional land this turn.
 *    Equip {2}"
 *
 * Covers the static grant and the trigger: the top two cards are exiled and playable, and exactly
 * one additional land drop is granted (a land already played from hand, one exiled land played as
 * the extra drop, the second exiled land refused).
 */
class SwordOfForgeAndFrontierScenarioTest : ScenarioTestBase() {

    private val equipAbilityId by lazy {
        cardRegistry.requireCard("Sword of Forge and Frontier").activatedAbilities[0].id
    }

    init {
        fun setUp(): TestGame {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(1, "Sword of Forge and Frontier")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInHand(1, "Plains")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            val sword = game.findPermanent("Sword of Forge and Frontier")!!
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = sword,
                    abilityId = equipAbilityId,
                    targets = listOf(ChosenTarget.Permanent(giant))
                )
            ).error shouldBe null
            game.resolveStack()
            return game
        }

        fun exiled(game: TestGame, name: String) =
            game.state.getExile(game.player1Id).single {
                game.state.getEntity(it)?.get<CardComponent>()?.name == name
            }

        test("equipped creature gets +2/+2 and protection from red and from green") {
            val game = setUp()
            val giant = game.findPermanent("Hill Giant")!!
            game.state.projectedState.getPower(giant) shouldBe 5
            game.state.projectedState.getToughness(giant) shouldBe 5
            game.state.projectedState.hasKeyword(giant, "PROTECTION_FROM_RED") shouldBe true
            game.state.projectedState.hasKeyword(giant, "PROTECTION_FROM_GREEN") shouldBe true
            game.state.projectedState.hasKeyword(giant, "PROTECTION_FROM_BLUE") shouldBe false
        }

        test("combat damage exiles the top two cards, playable this turn, plus exactly one extra land drop") {
            val game = setUp()
            val plains = game.findCardsInHand(1, "Plains").single()
            game.execute(PlayLand(game.player1Id, plains)).error shouldBe null

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hill Giant" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers()
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            game.resolveStack()

            withClue("opponent took 5 from the equipped Giant") {
                game.getLifeTotal(2) shouldBe 15
            }
            withClue("top two cards exiled, third stays in library") {
                game.isInExile(1, "Forest") shouldBe true
                game.isInExile(1, "Mountain") shouldBe true
                game.isInExile(1, "Island") shouldBe false
                game.librarySize(1) shouldBe 1
            }

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("the additional land drop lets an exiled land be played after the hand land") {
                game.execute(PlayLand(game.player1Id, exiled(game, "Forest"))).error shouldBe null
                game.isOnBattlefield("Forest") shouldBe true
            }
            withClue("only one additional land drop was granted") {
                game.execute(PlayLand(game.player1Id, exiled(game, "Mountain"))).error shouldNotBe null
                game.isInExile(1, "Mountain") shouldBe true
            }
        }
    }
}
