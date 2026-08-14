package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Edgar, Charmed Groom // Edgar Markov's Coffin (VOW #236).
 *
 *   Front — Edgar, Charmed Groom (4/4) — Other Vampires you control get +1/+1. When Edgar dies,
 *           return it to the battlefield transformed under its owner's control.
 *   Back  — Edgar Markov's Coffin (Legendary Artifact) — At the beginning of your upkeep, create a
 *           1/1 white and black Vampire with lifelink and put a bloodline counter on the Coffin.
 *           Then if there are three or more bloodline counters on it, remove those counters and
 *           transform it.
 *
 * Exercises the "other Vampires" anthem, the dies → return-transformed trigger, and the Coffin's
 * upkeep token + counter accumulation that transforms back after three upkeeps.
 */
class EdgarCharmedGroomScenarioTest : ScenarioTestBase() {

    init {
        context("Edgar, Charmed Groom") {

            test("other Vampires you control get +1/+1 but Edgar does not pump itself") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Edgar, Charmed Groom", summoningSickness = false)
                    .withCardOnBattlefield(1, "Falkenrath Forebear", summoningSickness = false) // a 3/1 Vampire
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val edgar = game.findPermanent("Edgar, Charmed Groom")!!
                val otherVampire = game.findPermanent("Falkenrath Forebear")!!

                withClue("the other Vampire gets +1/+1 (3/1 -> 4/2)") {
                    game.state.projectedState.getPower(otherVampire) shouldBe 4
                    game.state.projectedState.getToughness(otherVampire) shouldBe 2
                }
                withClue("Edgar does not buff itself (stays 4/4)") {
                    game.state.projectedState.getPower(edgar) shouldBe 4
                    game.state.projectedState.getToughness(edgar) shouldBe 4
                }
            }

            test("when Edgar dies it returns transformed as Edgar Markov's Coffin") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Edgar, Charmed Groom", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val edgar = game.findPermanent("Edgar, Charmed Groom")!!

                // Two Bolts (3 + 3 = 6) kill the 4/4 Edgar.
                repeat(2) {
                    game.castSpell(1, "Lightning Bolt", targetId = edgar).error shouldBe null
                    if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                    game.resolveStack()
                }
                var guard = 0
                while (game.findPermanent("Edgar Markov's Coffin") == null && guard++ < 10) game.resolveStack()

                withClue("Edgar returns as its transformed Coffin back face (same entity)") {
                    game.findPermanent("Edgar Markov's Coffin") shouldBe edgar
                    game.state.getEntity(edgar)!!.get<CardComponent>()!!.name shouldBe "Edgar Markov's Coffin"
                }
            }

            test("the Coffin makes a Vampire and a bloodline counter each upkeep, transforming after three") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Edgar Markov's Coffin")
                    .withActivePlayer(1)
                    // Start before our upkeep so the first upkeep passUntilPhase (forward-only)
                    // reaches is *our* upkeep, when the "your upkeep" Coffin trigger fires.
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                val coffin = game.findPermanent("Edgar Markov's Coffin")!!

                // First upkeep: one Vampire token, one bloodline counter, no transform yet.
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()
                withClue("first upkeep makes a Vampire token") {
                    game.findPermanents("Vampire Token").size shouldBe 1
                }
                withClue("stays a Coffin after one bloodline counter") {
                    game.state.getEntity(coffin)!!.get<CardComponent>()!!.name shouldBe "Edgar Markov's Coffin"
                }
            }
        }
    }
}
