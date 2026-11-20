package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.ths.cards.WitchesEye
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Witches' Eye (THS #222) — {1} Artifact — Equipment.
 *
 *   Equipped creature has "{1}, {T}: Scry 1."
 *   Equip {1}
 */
class WitchesEyeScenarioTest : ScenarioTestBase() {
    private val grantedId = WitchesEye.staticAbilities
        .filterIsInstance<GrantActivatedAbility>().single().ability.id

    init {
        test("the equipped creature taps and pays {1} to scry 1") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Witches' Eye", "Grizzly Bears")
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val top = game.state.getLibrary(game.player1Id).first()

            game.execute(ActivateAbility(game.player1Id, bears, grantedId)).error shouldBe null
            game.resolveStack()

            val scry = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("scry 1 looks at the top card") { scry.options shouldBe listOf(top) }
            game.selectCards(listOf(top)).error shouldBe null
            game.resolveStack()

            withClue("the scried card went to the bottom") {
                game.state.getLibrary(game.player1Id).last() shouldBe top
            }
            withClue("the {T} cost tapped the equipped creature") {
                game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
            }
        }
    }
}
