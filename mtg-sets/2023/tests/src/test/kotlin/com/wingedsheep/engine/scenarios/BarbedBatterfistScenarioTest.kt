package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.BarbedBatterfist
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Barbed Batterfist (ONE #121) — For Mirrodin! makes a 2/2 red Rebel and attaches to it;
 * the equipped creature gets +1/-1, so the Rebel is a 3/1.
 */
class BarbedBatterfistScenarioTest : ScenarioTestBase() {

    init {
        context("Barbed Batterfist") {
            test("For Mirrodin! creates a 2/2 red Rebel token and attaches the Equipment to it") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Barbed Batterfist")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Barbed Batterfist")
                withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Exactly one Rebel token") { game.findPermanents("Rebel Token").size shouldBe 1 }
                val rebel = game.findPermanent("Rebel Token")!!
                val batterfist = game.findPermanent("Barbed Batterfist")!!
                val rebelEntity = game.state.getEntity(rebel)!!
                val rebelCard = rebelEntity.get<CardComponent>()!!

                withClue("The Rebel is a red creature token") {
                    rebelEntity.has<TokenComponent>() shouldBe true
                    rebelCard.colors shouldBe setOf(Color.RED)
                    game.state.projectedState.isCreature(rebel) shouldBe true
                    game.state.projectedState.hasSubtype(rebel, "Rebel") shouldBe true
                }
                withClue("Batterfist is attached to the Rebel") {
                    game.state.getEntity(batterfist)?.get<AttachedToComponent>()?.targetId shouldBe rebel
                }
                withClue("Batterfist shows the For Mirrodin! keyword") {
                    BarbedBatterfist.keywords.contains(Keyword.FOR_MIRRODIN) shouldBe true
                }
                val projected = game.state.projectedState
                withClue("Equipped 2/2 Rebel is 3/1") {
                    projected.getPower(rebel) shouldBe 3
                    projected.getToughness(rebel) shouldBe 1
                }
            }
        }
    }
}
