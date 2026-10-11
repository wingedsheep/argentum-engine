package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.ltc.cards.MinasMorgulDarkFortress
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Minas Morgul, Dark Fortress (LTC #514) — "{3}{B}, {T}: Put a shadow counter on target creature.
 * For as long as that creature has a shadow counter on it, it's a Wraith in addition to its other
 * types."
 *
 * Pinned: the counter grants shadow (a keyword counter, CR 122.1b) and the Wraith type; both
 * outlast the turn; and removing the counter ends both — the type change is keyed to the counter,
 * not to Minas Morgul or to the end of turn.
 */
class MinasMorgulDarkFortressScenarioTest : ScenarioTestBase() {

    private val shadowAbilityId = MinasMorgulDarkFortress.activatedAbilities.first { !it.isManaAbility }.id

    private fun board(): TestGame = scenario()
        .withPlayers("Nazgûl", "Opponent")
        .withCardOnBattlefield(1, "Minas Morgul, Dark Fortress")
        .withLandsOnBattlefield(1, "Swamp", 4)
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.activateOnGiant() {
        val result = execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = findPermanent("Minas Morgul, Dark Fortress")!!,
                abilityId = shadowAbilityId,
                targets = listOf(ChosenTarget.Permanent(findPermanent("Hill Giant")!!))
            )
        )
        withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
        resolveStack()
    }

    init {
        test("the shadow counter makes the creature a Wraith with shadow") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            game.activateOnGiant()

            withClue("one shadow counter") {
                game.state.getEntity(giant)!!.get<CountersComponent>()!!.getCount(CounterType.SHADOW) shouldBe 1
            }
            withClue("the shadow counter grants shadow") {
                game.state.projectedState.hasKeyword(giant, Keyword.SHADOW) shouldBe true
            }
            withClue("it's a Wraith in addition to its other types") {
                game.state.projectedState.hasSubtype(giant, "Wraith") shouldBe true
                game.state.projectedState.hasSubtype(giant, "Giant") shouldBe true
            }

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            withClue("Grizzly Bears has no shadow, so it can't block the Giant") {
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldNotBe null
            }
        }

        test("removing the shadow counter ends both shadow and the Wraith type") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            game.activateOnGiant()
            game.state.projectedState.hasSubtype(giant, "Wraith") shouldBe true

            game.state = game.state.updateEntity(giant) { it.with(CountersComponent(emptyMap())) }

            withClue("no counter, no shadow") {
                game.state.projectedState.hasKeyword(giant, Keyword.SHADOW) shouldBe false
            }
            withClue("the Wraith type lasts only as long as the counter does") {
                game.state.projectedState.hasSubtype(giant, "Wraith") shouldBe false
            }
        }
    }
}
