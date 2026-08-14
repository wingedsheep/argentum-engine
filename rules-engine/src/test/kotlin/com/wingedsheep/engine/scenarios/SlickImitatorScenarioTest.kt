package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.mechanics.speed.SpeedService
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Speed
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Slick Imitator — "Max speed — {1}, Sacrifice this creature: Copy target spell you control. You
 * may choose new targets for the copy."
 *
 * The card is the first user of `Targets.SpellYouControl`, the type-unrestricted sibling of
 * `Targets.InstantOrSorcerySpellYouControl` / `Targets.CreatureSpellYouControl`. What that new
 * requirement has to get right:
 *
 * - a **permanent** spell you control is a legal target (the copy resolves into a token, CR 707.10f);
 * - "you control" actually excludes an opponent's spell;
 * - the max-speed gate still keeps the ability off the action list below speed 4.
 */
class SlickImitatorScenarioTest : ScenarioTestBase() {

    init {
        test("copies your own creature spell on the stack; the copy resolves as a token") {
            val game = imitatorGame(maxSpeed = true)

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.autoPayIfAsked()
            val bearsSpell = game.spellOnStack("Grizzly Bears")

            val activate = game.activateImitator(bearsSpell)
            withClue("Your own creature spell is a legal target: ${activate.error}") {
                activate.error shouldBe null
            }
            game.autoPayIfAsked()
            game.resolveStack()

            val bears = game.findAllPermanents("Grizzly Bears")
            withClue("The original and its copy both resolved — found ${bears.size}") {
                bears.size shouldBe 2
            }
            withClue("A copy of a permanent spell becomes a token") {
                bears.count { game.state.getEntity(it)?.has<TokenComponent>() == true } shouldBe 1
            }
            withClue("Slick Imitator sacrificed itself to pay the cost") {
                game.findPermanent("Slick Imitator") shouldBe null
                game.isInGraveyard(1, "Slick Imitator") shouldBe true
            }
        }

        test("an opponent's spell is not a legal target") {
            // The opponent's turn — a creature spell is sorcery-speed, so only they can cast one.
            val game = imitatorGame(maxSpeed = true, activePlayer = 2)

            // Opponent casts, then passes so player 1 holds priority with their spell on the stack.
            game.castSpell(2, "Grizzly Bears").error shouldBe null
            game.autoPayIfAsked()
            game.execute(PassPriority(game.player2Id))
            val bearsSpell = game.spellOnStack("Grizzly Bears")

            val activate = game.activateImitator(bearsSpell)
            withClue("\"target spell you control\" must reject the opponent's spell: ${activate.error}") {
                activate.error shouldNotBe null
            }
        }

        test("the ability is gated behind max speed") {
            val game = imitatorGame(maxSpeed = false)
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.autoPayIfAsked()
            val bearsSpell = game.spellOnStack("Grizzly Bears")

            val activate = game.activateImitator(bearsSpell)
            withClue("Speed is 1 from its own \"Start your engines!\", not 4: ${activate.error}") {
                game.state.speed(game.player1Id) shouldBe Speed.STARTING
                activate.error shouldNotBe null
            }
        }
    }

    private fun imitatorGame(maxSpeed: Boolean, activePlayer: Int = 1): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Slick Imitator", summoningSickness = false)
            .withLandsOnBattlefield(1, "Forest", 4)
            .withLandsOnBattlefield(2, "Forest", 3)
            .withCardInHand(1, "Grizzly Bears")
            .withCardInHand(2, "Grizzly Bears")
        repeat(12) {
            builder.withCardInLibrary(1, "Grizzly Bears")
            builder.withCardInLibrary(2, "Grizzly Bears")
        }
        val game = builder
            .withActivePlayer(activePlayer)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .build()
        game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (maxSpeed) {
            game.state = SpeedService.set(game.state, game.player1Id, Speed.MAX, "test").first
        }
        return game
    }

    /** The single stack object with the given card name. */
    private fun TestGame.spellOnStack(name: String): EntityId =
        state.stack.single { id: EntityId ->
            state.getEntity(id)?.get<CardComponent>()?.name == name
        }

    /** Activate Slick Imitator's single (max-speed-gated) activated ability at [spell]. */
    private fun TestGame.activateImitator(spell: EntityId): ExecutionResult {
        val imitator = findPermanent("Slick Imitator")!!
        val abilityId = cardRegistry.getCard("Slick Imitator")!!.script.activatedAbilities.single().id
        return execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = imitator,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Spell(spell))
            )
        )
    }

    /** Submit auto-pay if the engine paused for a mana-source decision. */
    private fun TestGame.autoPayIfAsked() {
        if (getPendingDecision() is com.wingedsheep.engine.core.SelectManaSourcesDecision) {
            submitManaSourcesAutoPay()
        }
    }
}
