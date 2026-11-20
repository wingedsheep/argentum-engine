package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.ths.cards.ReaperOfTheWilds
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Reaper of the Wilds (THS #201) — {2}{B}{G} Creature — Gorgon 4/5.
 * "Whenever another creature dies, scry 1.
 *  {B}: This creature gains deathtouch until end of turn.
 *  {1}{G}: This creature gains hexproof until end of turn."
 */
class ReaperOfTheWildsScenarioTest : ScenarioTestBase() {

    private val deathtouchAbilityId = ReaperOfTheWilds.activatedAbilities[0].id
    private val hexproofAbilityId = ReaperOfTheWilds.activatedAbilities[1].id

    init {
        test("another creature dying lets its controller scry 1") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Reaper of the Wilds")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val top = game.state.getLibrary(game.player1Id).first()
            game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()

            val scry = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("scry 1 looks at the top card") { scry.options shouldBe listOf(top) }
            game.selectCards(listOf(top)).error shouldBe null
            game.resolveStack()

            withClue("the scried card went to the bottom") {
                game.state.getLibrary(game.player1Id).last() shouldBe top
            }
        }

        test("{B} grants deathtouch and {1}{G} grants hexproof until end of turn") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Reaper of the Wilds")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val reaper = game.findPermanent("Reaper of the Wilds")!!

            game.state.projectedState.hasKeyword(reaper, Keyword.DEATHTOUCH) shouldBe false
            game.state.projectedState.hasKeyword(reaper, Keyword.HEXPROOF) shouldBe false

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = reaper, abilityId = deathtouchAbilityId)
            ).error shouldBe null
            game.resolveStack()
            withClue("deathtouch granted") {
                game.state.projectedState.hasKeyword(reaper, Keyword.DEATHTOUCH) shouldBe true
            }

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = reaper, abilityId = hexproofAbilityId)
            ).error shouldBe null
            game.resolveStack()
            withClue("hexproof granted") {
                game.state.projectedState.hasKeyword(reaper, Keyword.HEXPROOF) shouldBe true
            }
        }
    }
}
