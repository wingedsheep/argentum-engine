package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CreatedByComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.spm.cards.DocOckSinisterScientist
import com.wingedsheep.mtg.sets.definitions.spm.cards.MysterioMasterOfIllusion
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Mysterio, Master of Illusion (SPM #37) — {3}{U} Legendary Creature —
 * Human Villain, 3/3.
 *
 * "When Mysterio enters, create a 3/3 blue Illusion Villain creature token for each nontoken
 * Villain you control. Exile those tokens when Mysterio leaves the battlefield."
 *
 * Pins: (1) the ETB count is one token per *nontoken* Villain (Mysterio itself counts), and the
 * token is a 3/3 blue Illusion Villain; (2) the leaves trigger exiles *exactly* the tokens this
 * Mysterio created (provenance via `stampCreator`/`createdBySource`) — a same-typed Illusion Villain
 * token created by a different source survives, proving it is not "exile all your Illusions".
 */
class MysterioMasterOfIllusionScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    // A {0} sorcery that mints an unstamped 3/3 blue Illusion Villain token — a decoy with the same
    // type line as Mysterio's tokens but no provenance link, so it must NOT be exiled when Mysterio
    // leaves.
    private val decoyIllusionist = card("Decoy Illusionist") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            effect = CreateTokenEffect(
                power = 3,
                toughness = 3,
                colors = setOf(Color.BLUE),
                creatureTypes = setOf("Illusion", "Villain"),
                name = "Illusion Villain",
            )
        }
    }

    init {
        cardRegistry.register(listOf(MysterioMasterOfIllusion, DocOckSinisterScientist, decoyIllusionist))

        fun illusionTokens(game: TestGame): List<EntityId> = game.findPermanents("Illusion Villain")

        context("Mysterio, Master of Illusion") {

            test("ETB with no other Villain makes exactly one 3/3 blue Illusion Villain (Mysterio counts itself)") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardInHand(1, "Mysterio, Master of Illusion")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Mysterio, Master of Illusion")
                withClue("Mysterio should cast: ${cast.error}") { cast.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()
                game.resolveStack()

                val mysterio = game.findPermanent("Mysterio, Master of Illusion")
                    ?: error("Mysterio did not resolve onto the battlefield")

                val tokens = illusionTokens(game)
                tokens shouldHaveSize 1
                val token = tokens.single()
                val cc = game.state.getEntity(token)!!.get<CardComponent>()!!

                val p = projector.project(game.state)
                withClue("token is 3/3") {
                    p.getPower(token) shouldBe 3
                    p.getToughness(token) shouldBe 3
                }
                withClue("token is blue") { cc.colors shouldBe setOf(Color.BLUE) }
                withClue("token is an Illusion Villain") {
                    p.hasSubtype(token, "Illusion") shouldBe true
                    p.hasSubtype(token, "Villain") shouldBe true
                }
                withClue("token is stamped as created by this Mysterio") {
                    game.state.getEntity(token)!!.get<CreatedByComponent>()?.creatorId shouldBe mysterio
                }
            }

            test("ETB makes one token per nontoken Villain you control") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardInHand(1, "Mysterio, Master of Illusion")
                    .withCardOnBattlefield(1, "Doc Ock, Sinister Scientist")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Mysterio, Master of Illusion")
                withClue("Mysterio should cast: ${cast.error}") { cast.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()
                game.resolveStack()

                // Doc Ock + Mysterio = two nontoken Villains -> two tokens.
                illusionTokens(game) shouldHaveSize 2
            }

            test("leaves-the-battlefield exiles exactly Mysterio's own tokens, sparing an unrelated Illusion Villain") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardInHand(1, "Decoy Illusionist")
                    .withCardInHand(1, "Mysterio, Master of Illusion")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Decoy: an unstamped Illusion Villain token that must survive Mysterio leaving.
                game.castSpell(1, "Decoy Illusionist").error shouldBe null
                game.resolveStack()
                illusionTokens(game) shouldHaveSize 1
                val decoyToken = illusionTokens(game).single()

                // Mysterio enters: creates one stamped Illusion Villain (only nontoken Villain is Mysterio;
                // the decoy is a token so it doesn't count).
                val cast = game.castSpell(1, "Mysterio, Master of Illusion")
                withClue("Mysterio should cast: ${cast.error}") { cast.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()
                game.resolveStack()
                val mysterio = game.findPermanent("Mysterio, Master of Illusion")
                    ?: error("Mysterio did not resolve onto the battlefield")
                illusionTokens(game) shouldHaveSize 2
                val mysterioToken = illusionTokens(game).first { it != decoyToken }

                // Kill Mysterio through the engine so the leaves trigger fires (3 damage to a 3/3).
                val bolt = game.castSpell(1, "Lightning Bolt", targetId = mysterio)
                withClue("Bolt should cast: ${bolt.error}") { bolt.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()
                game.resolveStack()

                withClue("Mysterio is gone") { game.findPermanent("Mysterio, Master of Illusion") shouldBe null }
                withClue("Mysterio's own token was exiled; the decoy survives") {
                    val remaining = illusionTokens(game)
                    remaining shouldHaveSize 1
                    remaining.single() shouldBe decoyToken
                    // The exiled token ceases to exist (Rule 111.7): gone from the battlefield entirely.
                    game.state.getBattlefield().contains(mysterioToken) shouldBe false
                }
            }
        }
    }
}
