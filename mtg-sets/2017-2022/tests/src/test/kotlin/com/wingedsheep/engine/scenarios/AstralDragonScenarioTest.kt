package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Astral Dragon (CLB #613) — {6}{U}{U} Creature — Dragon, 4/4.
 *
 *   Flying
 *   Project Image — When this creature enters, create two tokens that are copies of target
 *   noncreature permanent, except they're 3/3 Dragon creatures in addition to their other types,
 *   and they have flying.
 *
 * Covers the copy exceptions on a real permanent, and the card's ruling that targeting an Aura
 * creates nothing: the copies would be creature Auras, which can't enchant anything
 * (CR 303.4d), so no host is legal and the tokens aren't created (CR 303.4g).
 */
class AstralDragonScenarioTest : ScenarioTestBase() {

    init {
        context("Project Image") {

            test("copies a noncreature artifact as two 3/3 flying Dragon artifact creatures") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Astral Dragon")
                    .withCardOnBattlefield(2, "Sol Ring", tapped = true)
                    .withLandsOnBattlefield(1, "Island", 8)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ring = game.findPermanent("Sol Ring")!!

                game.castSpell(1, "Astral Dragon").error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(ring)).error shouldBe null
                game.resolveStack()

                val tokens = game.findAllPermanents("Sol Ring").filter { it != ring }
                withClue("two token copies are created") { tokens.size shouldBe 2 }

                val projected = game.state.projectedState
                for (token in tokens) {
                    withClue("each copy is a token") {
                        game.state.getEntity(token)?.has<TokenComponent>() shouldBe true
                    }
                    withClue("each copy is a 3/3") {
                        projected.getPower(token) shouldBe 3
                        projected.getToughness(token) shouldBe 3
                    }
                    withClue("each copy keeps its artifact type and becomes a Dragon creature") {
                        projected.isCreature(token) shouldBe true
                        projected.getTypes(token).contains("ARTIFACT") shouldBe true
                        projected.hasSubtype(token, "Dragon") shouldBe true
                    }
                    withClue("each copy has flying") {
                        projected.hasKeyword(token, Keyword.FLYING) shouldBe true
                    }
                    withClue("copies don't inherit the original's tapped state") {
                        game.state.getEntity(token)
                            ?.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>() shouldBe false
                    }
                }
            }

            test("targeting an Aura creates no tokens") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Astral Dragon")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardAttachedTo(2, "Holy Strength", "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 8)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val aura = game.findPermanent("Holy Strength")!!

                game.castSpell(1, "Astral Dragon").error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(aura)).error shouldBe null
                game.resolveStack()

                withClue("no host choice is offered — a creature Aura can't enchant anything") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("only the original Aura exists") {
                    game.findAllPermanents("Holy Strength") shouldBe listOf(aura)
                }
            }
        }
    }
}
