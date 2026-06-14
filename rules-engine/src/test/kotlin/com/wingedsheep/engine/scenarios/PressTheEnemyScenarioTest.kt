package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Press the Enemy:
 *   {2}{U}{U} Instant — "Return target spell or nonland permanent an opponent controls to its
 *   owner's hand. You may cast an instant or sorcery spell with equal or lesser mana value from
 *   your hand without paying its mana cost."
 *
 * Covers (a) bouncing a nonland permanent an opponent controls then free-casting an instant/sorcery
 * whose mana value is within the cap (and a too-expensive one not being offered), and (b) bouncing
 * a spell on the stack so it does not resolve.
 */
class PressTheEnemyScenarioTest : ScenarioTestBase() {

    init {
        // A mana-value-3 creature for the opponent to control / cast (the MV cap source).
        cardRegistry.register(
            CardDefinition.creature(
                name = "Test Ogre",
                manaCost = ManaCost.parse("{2}{R}"),
                subtypes = setOf(Subtype("Ogre")),
                power = 3,
                toughness = 3
            )
        )
        // A cheap sorcery (MV 1) that draws a card — within the MV-3 cap, observable via card draw.
        cardRegistry.register(
            CardDefinition.sorcery(
                name = "Cheap Draw",
                manaCost = ManaCost.parse("{U}"),
                oracleText = "Draw a card.",
                script = CardScript(spellEffect = Effects.DrawCards(1))
            )
        )
        // An expensive sorcery (MV 5) — above the MV-3 cap, must NOT be free-castable.
        cardRegistry.register(
            CardDefinition.sorcery(
                name = "Expensive Draw",
                manaCost = ManaCost.parse("{4}{U}"),
                oracleText = "Draw a card.",
                script = CardScript(spellEffect = Effects.DrawCards(1))
            )
        )

        context("Press the Enemy — bounce a nonland permanent an opponent controls") {

            test("free-cast an instant/sorcery with MV <= the bounced permanent's MV") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Press the Enemy")
                    .withCardInHand(1, "Cheap Draw")
                    .withCardInLibrary(1, "Test Ogre") // something to draw, proving the free cast resolved
                    .withCardOnBattlefield(2, "Test Ogre")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ogreId = game.findPermanent("Test Ogre") ?: error("Test Ogre not on battlefield")

                game.castSpell(1, "Press the Enemy", ogreId).error shouldBe null
                game.resolveStack()

                withClue("Opponent's creature returned to its owner's hand") {
                    game.isInHand(2, "Test Ogre") shouldBe true
                    game.isOnBattlefield("Test Ogre") shouldBe false
                }

                // The free cast is optional — accept it.
                game.answerYesNo(true)
                // Cheap Draw is a targetless sorcery — resolve it.
                game.resolveStack()

                withClue("Cheap Draw (MV 1 <= 3) was cast for free and resolved (drew a card)") {
                    game.isInHand(1, "Cheap Draw") shouldBe false
                    game.isInGraveyard(1, "Cheap Draw") shouldBe true
                }
            }

            test("a too-expensive instant/sorcery is NOT offered for the free cast") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Press the Enemy")
                    .withCardInHand(1, "Expensive Draw")
                    .withCardOnBattlefield(2, "Test Ogre")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ogreId = game.findPermanent("Test Ogre") ?: error("Test Ogre not on battlefield")

                game.castSpell(1, "Press the Enemy", ogreId).error shouldBe null
                game.resolveStack()

                withClue("Opponent's creature still returned to hand") {
                    game.isInHand(2, "Test Ogre") shouldBe true
                }
                withClue("No free-cast decision was offered (MV 5 > 3)") {
                    game.state.pendingDecision shouldBe null
                }
                withClue("Expensive Draw stays in hand, uncast") {
                    game.isInHand(1, "Expensive Draw") shouldBe true
                    game.isOnBattlefield("Expensive Draw") shouldBe false
                }
            }
        }

        context("Press the Enemy — bounce a spell on the stack") {

            test("returns the opponent's spell to its owner's hand; it does not resolve") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Press the Enemy")
                    .withCardInHand(2, "Test Ogre")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withLandsOnBattlefield(2, "Mountain", 3)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Opponent casts a creature spell; it goes on the stack.
                game.castSpell(2, "Test Ogre").error shouldBe null
                game.state.stack.any {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Test Ogre"
                } shouldBe true

                // Active player passes priority with the spell on the stack; Player1 now responds.
                game.passPriority()

                // In response, Player1 bounces the spell with Press the Enemy.
                game.castSpellTargetingStackSpell(1, "Press the Enemy", "Test Ogre").error shouldBe null
                game.resolveStack()

                withClue("The spell was removed from the stack to its owner's hand (did not resolve)") {
                    game.isInHand(2, "Test Ogre") shouldBe true
                    game.isOnBattlefield("Test Ogre") shouldBe false
                }
            }
        }
    }
}
