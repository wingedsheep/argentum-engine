package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tough Cookie (WOE #193) — {1}{G} Artifact Creature — Food Golem 2/2.
 *
 * "When this creature enters, create a Food token.
 *  {2}{G}: Until end of turn, target noncreature artifact you control becomes a 4/4 artifact creature.
 *  {2}, {T}, Sacrifice this creature: You gain 3 life."
 */
class ToughCookieScenarioTest : ScenarioTestBase() {

    private val animateAbilityId by lazy {
        cardRegistry.requireCard("Tough Cookie").activatedAbilities[0].id
    }
    private val sacrificeAbilityId by lazy {
        cardRegistry.requireCard("Tough Cookie").activatedAbilities[1].id
    }

    init {
        test("entering the battlefield creates a Food token") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Tough Cookie")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Tough Cookie").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Tough Cookie") shouldBe true
            withClue("the enters trigger made a Food") {
                game.findPermanent("Food") shouldNotBe null
            }
        }

        test("the animate ability turns a Food token into a 4/4 that is still a Food") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Tough Cookie", summoningSickness = false)
                .withCardOnBattlefield(1, "Food")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cookie = game.findPermanent("Tough Cookie")!!
            val food = game.findPermanent("Food")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = cookie,
                    abilityId = animateAbilityId,
                    targets = listOf(ChosenTarget.Permanent(food))
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("the Food became a 4/4 artifact creature") {
                game.state.projectedState.isCreature(food) shouldBe true
                game.state.projectedState.getPower(food) shouldBe 4
                game.state.projectedState.getToughness(food) shouldBe 4
            }
            withClue("it kept its other types and subtypes (2023-09-01 ruling)") {
                game.state.projectedState.hasSubtype(food, "Food") shouldBe true
            }
        }

        test("the animate ability can't target a creature artifact — including Tough Cookie itself") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Tough Cookie", summoningSickness = false)
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cookie = game.findPermanent("Tough Cookie")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = cookie,
                    abilityId = animateAbilityId,
                    targets = listOf(ChosenTarget.Permanent(cookie))
                )
            ).error shouldNotBe null
        }

        test("{2}, {T}, sacrifice: gain 3 life — Tough Cookie is its own Food outlet") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Tough Cookie", summoningSickness = false)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withLifeTotal(1, 20)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cookie = game.findPermanent("Tough Cookie")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = cookie,
                    abilityId = sacrificeAbilityId
                )
            ).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 23
            game.isInGraveyard(1, "Tough Cookie") shouldBe true
        }
    }
}
