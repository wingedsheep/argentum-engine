package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Titania, Gaea Incarnate (BRO #256b) — meld result of Titania, Voice of Gaea + Argoth, Sanctum of
 * Nature. Legendary Creature — Elemental Avatar, * / *.
 *
 *   Titania's power and toughness are each equal to the number of lands you control.
 *   {3}{G}: Put four +1/+1 counters on target land you control. It becomes a 0/0 Elemental
 *   creature with haste. It's still a land.
 *
 * The meld itself (and the enters trigger returning graveyard lands) is covered by
 * [TitaniaVoiceOfGaeaScenarioTest]; this file pins the CDA and the land animation, which has no
 * duration and so outlasts the turn.
 */
class TitaniaGaeaIncarnateScenarioTest : ScenarioTestBase() {

    init {
        test("P/T track lands you control; {3}{G} animates a land into a 4/4 hasty Elemental for good") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Titania, Gaea Incarnate")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val titania = game.findPermanent("Titania, Gaea Incarnate")!!
            game.state.projectedState.getPower(titania) shouldBe 5
            game.state.projectedState.getToughness(titania) shouldBe 5

            val target = game.findPermanents("Forest").first()
            val abilityId = cardRegistry.getCard("Titania, Gaea Incarnate")!!.script.activatedAbilities[0].id
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = titania,
                    abilityId = abilityId,
                    targets = listOf(entityIdToChosenTarget(game.state, target)),
                )
            ).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            withClue("a 0/0 with four +1/+1 counters, an Elemental creature land with haste") {
                projected.isCreature(target) shouldBe true
                projected.hasType(target, "LAND") shouldBe true
                projected.hasSubtype(target, "Elemental") shouldBe true
                projected.hasKeyword(target, Keyword.HASTE) shouldBe true
                projected.getPower(target) shouldBe 4
                projected.getToughness(target) shouldBe 4
            }
            withClue("it is still a land, so Titania still counts five") {
                projected.getPower(titania) shouldBe 5
            }

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("no duration: the land is still a creature next turn") {
                game.state.projectedState.isCreature(target) shouldBe true
            }
        }
    }
}
