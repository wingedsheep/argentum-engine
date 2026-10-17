package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Three Dog, Galaxy News DJ {1}{R}{W} — Legendary Creature — Human Bard 1/5
 *   Whenever you attack, you may pay {2} and sacrifice an Aura attached to Three Dog. When you
 *   sacrifice an Aura this way, for each other attacking creature you control, create a token
 *   that's a copy of that Aura attached to that creature.
 *
 * The engine-level matrix (sacrificed tokens, protection, shroud) lives in
 * SacrificedPermanentTokenCopyScenarioTest; this pins the card: the {2} is paid, Three Dog itself
 * gets no copy, the may can be declined, and it isn't offered without the {2}.
 */
class ThreeDogGalaxyNewsDjScenarioTest : ScenarioTestBase() {

    private fun TestGame.named(name: String): List<EntityId> =
        state.getBattlefield().filter { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun TestGame.hostOf(id: EntityId): EntityId? = state.getEntity(id)?.get<AttachedToComponent>()?.targetId

    /** Resolve the attack trigger, answering the may with [pay]; returns whether it was asked. */
    private fun TestGame.resolveAnswering(pay: Boolean): Boolean {
        var asked = false
        var guard = 0
        while (guard++ < 30 && (state.stack.isNotEmpty() || state.pendingDecision != null)) {
            when (val decision = state.pendingDecision) {
                null -> resolveStack()
                is YesNoDecision -> {
                    asked = true
                    submitDecision(YesNoResponse(decision.id, pay)).error shouldBe null
                }
                is SelectManaSourcesDecision ->
                    submitDecision(ManaSourcesSelectedResponse(decision.id, emptyList(), true)).error shouldBe null
                is SelectCardsDecision -> selectCards(decision.options.take(1)).error shouldBe null
                else -> error("unexpected decision $decision")
            }
        }
        return asked
    }

    private fun board(lands: Int) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Three Dog, Galaxy News DJ")
        .withCardAttachedTo(1, "Holy Strength", "Three Dog, Galaxy News DJ")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Hill Giant")
        .withLandsOnBattlefield(1, "Plains", lands)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.attackWithAll() {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(
            mapOf("Three Dog, Galaxy News DJ" to 2, "Grizzly Bears" to 2, "Hill Giant" to 2)
        ).error shouldBe null
    }

    init {
        test("paying {2} and sacrificing the Aura copies it onto each other attacker") {
            val game = board(lands = 2)
            game.attackWithAll()
            game.resolveAnswering(pay = true) shouldBe true

            withClue("the {2} was paid") {
                game.findAllPermanents("Plains").all { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
            }
            withClue("the Aura was sacrificed") { game.isInGraveyard(1, "Holy Strength") shouldBe true }
            withClue("a copy on each other attacker, none on Three Dog") {
                game.named("Holy Strength").map { game.hostOf(it) } shouldContainExactlyInAnyOrder
                    listOf(game.findPermanent("Grizzly Bears"), game.findPermanent("Hill Giant"))
            }
            game.state.projectedState.getToughness(game.findPermanent("Hill Giant")!!) shouldBe 5
        }

        test("declining keeps the Aura on Three Dog and makes no tokens") {
            val game = board(lands = 2)
            game.attackWithAll()
            game.resolveAnswering(pay = false) shouldBe true

            val aura = game.named("Holy Strength").single()
            game.hostOf(aura) shouldBe game.findPermanent("Three Dog, Galaxy News DJ")
        }

        test("an Aura on another attacker, not Three Dog, isn't offered and nothing is paid") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Three Dog, Galaxy News DJ")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.attackWithAll()
            game.resolveAnswering(pay = true) shouldBe false

            withClue("the lands stay untapped") {
                game.findAllPermanents("Plains").none { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
            }
            val aura = game.named("Holy Strength").single()
            game.hostOf(aura) shouldBe game.findPermanent("Grizzly Bears")
        }

        test("without {2} available the may isn't offered") {
            val game = board(lands = 1)
            game.attackWithAll()
            game.resolveAnswering(pay = true) shouldBe false

            val aura = game.named("Holy Strength").single()
            game.hostOf(aura) shouldBe game.findPermanent("Three Dog, Galaxy News DJ")
        }
    }
}
