package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.afr.cards.GelatinousCube
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Gelatinous Cube — {2}{B}{B} Creature — Ooze 4/3 (AFR #105).
 *
 * "Engulf — When this creature enters, exile target non-Ooze creature an opponent controls until
 * this creature leaves the battlefield.
 * Dissolve — {X}{B}: Put target creature card with mana value X exiled with this creature into its
 * owner's graveyard."
 *
 * Focus: the linked exile — Engulf's exile returns when the Cube leaves, Dissolve can only reach
 * cards in that pile whose mana value equals the X paid, and a dissolved card stays in the
 * graveyard when the Cube later leaves (ruling).
 */
class GelatinousCubeScenarioTest : ScenarioTestBase() {

    private val dissolveId = GelatinousCube.activatedAbilities[0].id

    private fun board() = scenario()
        .withPlayers("Caster", "Opponent")
        .withCardInHand(1, "Gelatinous Cube")
        .withCardInHand(1, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Swamp", 7)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, "Ancient Ooze")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.engulfBears(): EntityId {
        val bears = findPermanent("Grizzly Bears")!!
        castSpell(1, "Gelatinous Cube").error shouldBe null
        resolveStack()
        val decision = state.pendingDecision as ChooseTargetsDecision
        withClue("only the non-Ooze creature an opponent controls is a legal Engulf target") {
            decision.legalTargets.values.flatten().toSet() shouldBe setOf(bears)
        }
        selectTargets(listOf(bears)).error shouldBe null
        resolveStack()
        withClue("Grizzly Bears is engulfed") { isInExile(2, "Grizzly Bears") shouldBe true }
        return bears
    }

    private fun TestGame.dissolve(x: Int): ExecutionResult {
        val cube = findPermanent("Gelatinous Cube")!!
        val exiled = state.getExile(player2Id).first { id ->
            state.getEntity(id)?.get<CardComponent>()
                ?.name == "Grizzly Bears"
        }
        val result = execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = cube,
                abilityId = dissolveId,
                xValue = x,
                targets = listOf(ChosenTarget.Card(cardId = exiled, ownerId = player2Id, zone = Zone.EXILE))
            )
        )
        if (result.error == null && getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
        return result
    }

    private fun TestGame.boltCube() {
        val cube = findPermanent("Gelatinous Cube")!!
        castSpell(1, "Lightning Bolt", targetId = cube).error shouldBe null
        resolveStack()
        isOnBattlefield("Gelatinous Cube") shouldBe false
    }

    init {
        test("the engulfed creature returns when Gelatinous Cube leaves the battlefield") {
            val game = board()
            game.engulfBears()

            game.boltCube()

            withClue("Grizzly Bears is back under its owner's control") {
                game.isInExile(2, "Grizzly Bears") shouldBe false
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }

        test("Dissolve puts the engulfed card into its owner's graveyard, and it doesn't come back") {
            val game = board()
            game.engulfBears()

            game.dissolve(x = 2).error shouldBe null
            game.resolveStack()
            withClue("dissolved into the opponent's graveyard") {
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.isInExile(2, "Grizzly Bears") shouldBe false
            }

            game.boltCube()
            withClue("a card that left exile isn't returned when the Cube leaves") {
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }
        }

        test("Dissolve can't target an engulfed card whose mana value isn't X") {
            val game = board()
            game.engulfBears()

            withClue("Grizzly Bears has mana value 2, so X = 3 is rejected") {
                game.dissolve(x = 3).error shouldNotBe null
            }
            game.isInExile(2, "Grizzly Bears") shouldBe true
        }
    }
}
