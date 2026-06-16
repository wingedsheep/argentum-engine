package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain as stringShouldContain

/**
 * An Omen card (Tarkir: Dragonstorm) is "just the Dragon" in every zone except the stack. When its
 * Omen face is cast, the spell sitting on the stack must be presented to the client as the *Omen*
 * (e.g. Petty Revenge — a Sorcery), not as the Dragon creature. Regression for the bug where casting
 * the Omen showed the creature's name/type/oracle-text/P-T on the stack, so the cast spell appeared
 * to vanish into the Dragon and then (correctly) shuffle into the library.
 */
class OmenStackDisplayTest : ScenarioTestBase() {

    private val transformer = com.wingedsheep.engine.view.ClientStateTransformer(cardRegistry)
    private val p1 = EntityId.of("player-1")

    init {
        test("Omen face on the stack is shown as the Omen spell, not the Dragon") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Disruptive Stormbrood")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val cardId = game.state.getHand(p1).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Disruptive Stormbrood"
            }
            game.execute(CastSpell(p1, cardId, listOf(ChosenTarget.Permanent(bears)), faceIndex = 0)).error shouldBe null

            val stackId = game.state.stack.first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Disruptive Stormbrood"
            }
            val card = transformer.transform(game.state, p1).cards[stackId]!!

            withClue("name should be the Omen, not the Dragon") { card.name shouldBe "Petty Revenge" }
            withClue("type line should be the Omen sorcery") { card.typeLine.stringShouldContain("Sorcery") }
            withClue("Omen face is a sorcery — no P/T") {
                card.power.shouldBeNull()
                card.toughness.shouldBeNull()
            }
            withClue("cast face cost {1}{B} is black") { card.colors shouldContain com.wingedsheep.sdk.core.Color.BLACK }
            withClue("oracle text is the Omen's effect") {
                card.oracleText.stringShouldContain("Destroy target creature")
            }
        }

        test("creature face on the stack is still shown as the Dragon") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Disruptive Stormbrood")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Disruptive Stormbrood").error shouldBe null
            val stackId = game.state.stack.first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Disruptive Stormbrood"
            }
            val card = transformer.transform(game.state, p1).cards[stackId]!!

            withClue("creature face keeps the Dragon characteristics") {
                card.name shouldBe "Disruptive Stormbrood"
                card.typeLine.stringShouldContain("Creature")
                card.power shouldBe 3
                card.toughness shouldBe 3
            }
        }
    }
}
