package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Display of Dominance — {1}{G} Instant (DTK #182).
 *
 * "Choose one — • Destroy target blue or black noncreature permanent. • Permanents you control
 * can't be the targets of blue or black spells your opponents control this turn."
 *
 * The second mode is a rule about the game, not an ability of the permanents (the card's
 * rulings): it reaches permanents that arrive later that turn, it stops only *spells*, and a spell
 * already aimed at one finds that target illegal as it resolves.
 */
class DisplayOfDominanceScenarioTest : ScenarioTestBase() {

    private val shieldMode = 1

    /** Player 1 (active) holds Display of Dominance; player 2 holds blue, black and red answers. */
    private fun game(): TestGame = scenario()
        .withPlayers("Dominator", "Opponent")
        .withCardInHand(1, "Display of Dominance")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Forest", 4)
        .withCardInHand(2, "Unsummon")
        .withCardInHand(2, "Doom Blade")
        .withCardInHand(2, "Shock")
        .withLandsOnBattlefield(2, "Island", 1)
        .withLandsOnBattlefield(2, "Swamp", 2)
        .withLandsOnBattlefield(2, "Mountain", 1)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /** Cast the shield mode, resolve it, then hand player 2 priority in the same step. */
    private fun TestGame.raiseShield() {
        castSpellWithMode(1, "Display of Dominance", shieldMode).error shouldBe null
        resolveStack()
        passPriority()
    }

    private fun TestGame.unsummonTargets(): List<com.wingedsheep.sdk.model.EntityId> =
        getLegalActions(2).firstOrNull { it.description.contains("Unsummon") }?.let { action ->
            action.targetRequirements?.firstOrNull()?.validTargets ?: action.validTargets.orEmpty()
        }.orEmpty()

    init {
        test("opponents' blue and black spells can't target your permanents") {
            val game = game()
            game.raiseShield()
            val bears = game.findPermanent("Grizzly Bears")!!

            withClue("Unsummon isn't offered the Bears as a target") { game.unsummonTargets() shouldNotContain bears }
            game.castSpell(2, "Unsummon", bears).error shouldNotBe null
            game.castSpell(2, "Doom Blade", bears).error shouldNotBe null
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("a red spell may still target them") {
            val game = game()
            game.raiseShield()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(2, "Shock", bears).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Grizzly Bears") shouldBe false
        }

        test("abilities from blue sources may still target them") {
            val game = scenario()
                .withPlayers("Dominator", "Opponent")
                .withCardInHand(1, "Display of Dominance")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardOnBattlefield(2, "Prodigal Sorcerer")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.raiseShield()
            val bears = game.findPermanent("Grizzly Bears")!!
            val sorcerer = game.findPermanent("Prodigal Sorcerer")!!
            val abilityId = cardRegistry.getCard("Prodigal Sorcerer")!!.script.activatedAbilities[0].id

            game.execute(
                ActivateAbility(
                    playerId = game.player2Id,
                    sourceId = sorcerer,
                    abilityId = abilityId,
                    targets = listOf(entityIdToChosenTarget(game.state, bears))
                )
            ).error shouldBe null
            game.resolveStack()
            game.state.getEntity(bears)
                ?.get<com.wingedsheep.engine.state.components.battlefield.DamageComponent>()
                .shouldNotBeNull().amount shouldBe 1
        }

        test("it covers a permanent that comes under your control later that turn") {
            val game = scenario()
                .withPlayers("Dominator", "Opponent")
                .withCardInHand(1, "Display of Dominance")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardInHand(2, "Unsummon")
                .withLandsOnBattlefield(2, "Island", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpellWithMode(1, "Display of Dominance", shieldMode).error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.passPriority()

            val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
            game.castSpell(2, "Unsummon", bears).error shouldNotBe null
        }

        test("a blue spell already targeting your permanent fizzles when the shield resolves first") {
            val game = game()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.passPriority()
            game.castSpell(2, "Unsummon", bears).error shouldBe null
            game.passPriority()
            withClue("player 1 responds with the shield, which resolves first") {
                game.castSpellWithMode(1, "Display of Dominance", shieldMode).error shouldBe null
            }
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Unsummon") shouldBe true
        }

        test("your own blue spells may still target your permanents") {
            val game = scenario()
                .withPlayers("Dominator", "Opponent")
                .withCardInHand(1, "Display of Dominance")
                .withCardInHand(1, "Unsummon")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withLandsOnBattlefield(1, "Island", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpellWithMode(1, "Display of Dominance", shieldMode).error shouldBe null
            game.resolveStack()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Unsummon", bears).error shouldBe null
            game.resolveStack()
            game.isInHand(1, "Grizzly Bears") shouldBe true
        }

        test("the shield ends with the turn") {
            val game = game()
            game.raiseShield()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(2, "Unsummon", bears).error shouldNotBe null

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            withClue("on the opponent's turn the Bears are fair game again") {
                game.state.activePlayerId shouldBe game.player2Id
                game.unsummonTargets() shouldContain bears
                game.castSpell(2, "Unsummon", bears).error shouldBe null
            }
        }

        test("the first mode destroys a blue or black noncreature permanent, not a creature") {
            val game = scenario()
                .withPlayers("Dominator", "Opponent")
                .withCardInHand(1, "Display of Dominance")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardOnBattlefield(2, "Propaganda")
                .withCardOnBattlefield(2, "Prodigal Sorcerer")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val propaganda = game.findPermanent("Propaganda")!!
            val sorcerer = game.findPermanent("Prodigal Sorcerer")!!

            game.castSpellWithMode(1, "Display of Dominance", 0, sorcerer).error shouldNotBe null
            game.castSpellWithMode(1, "Display of Dominance", 0, propaganda).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Propaganda") shouldBe false
        }
    }
}
