package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Glamdring, Foe-hammer // Gleam of Death — {2} Legendary Artifact — Equipment.
 *
 *   Instant and sorcery spells you cast cost {X} less to cast, where X is equipped creature's power.
 *   Equip {2}
 *   Gleam of Death — {3}{U} Sorcery — Adventure: Mill six cards, then put all instant and sorcery
 *   cards from among them into your hand.
 *
 * The reduction is the first `CostReductionSource` that reads a single permanent reached through the
 * *reducing permanent's own attachment* instead of aggregating over what the caster controls, so it
 * needed the ability's source id threaded into `CostCalculator.evaluateReduction`. The tests pin the
 * three things that can go wrong with that: it reads the attached creature (not the caster's board),
 * it reads the right *amount* (power, not a flat 1), and it reads 0 rather than throwing when
 * Glamdring is equipping nothing.
 *
 * Divination is {2}{U}, so with one Island in play it is castable exactly when the reduction is 2 or
 * more — which makes affordability a clean proxy for the reduction amount.
 */
class GlamdringFoeHammerScenarioTest : ScenarioTestBase() {

    init {

        test("equipped creature's power pays for the generic half of an instant or sorcery") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Hill Giant") // 3/3
                .withCardAttachedTo(1, "Glamdring, Foe-hammer", "Hill Giant")
                .withCardInHand(1, "Divination")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Island", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            withClue("{2}{U} minus the 3/3's power leaves {U}, payable by the single Island") {
                game.castSpell(1, "Divination").error shouldBe null
            }
            game.resolveStack()
            game.handSize(1) shouldBe 2
        }

        test("the amount is the equipped creature's power, not a flat discount") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Devoted Hero") // 1/2
                .withCardAttachedTo(1, "Glamdring, Foe-hammer", "Devoted Hero")
                .withCardInHand(1, "Divination")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Island", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            withClue("power 1 shaves only {1}, leaving {1}{U} — one Island can't pay it") {
                game.castSpell(1, "Divination").error.shouldNotBeNull()
            }
        }

        test("an unequipped Glamdring reduces nothing") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Glamdring, Foe-hammer")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInHand(1, "Divination")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Island", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            withClue("nothing is attached, so X is 0 — the board's Hill Giant must not count") {
                game.castSpell(1, "Divination").error.shouldNotBeNull()
            }
        }

        test("Gleam of Death mills six and takes every instant and sorcery from among them") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Glamdring, Foe-hammer")
                .withCardInLibrary(1, "Divination")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Divination")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Island", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val glamdring = game.findCardsInHand(1, "Glamdring, Foe-hammer").single()

            game.execute(
                CastSpell(playerId = game.player1Id, cardId = glamdring, faceIndex = 0)
            ).error shouldBe null
            game.resolveStack()

            withClue("all six cards were milled") {
                game.librarySize(1) shouldBe 0
            }
            withClue("both Divinations were pulled out of the milled six — 'all', not 'up to one'") {
                game.handSize(1) shouldBe 2
                game.findCardsInHand(1, "Divination").size shouldBe 2
            }
            withClue("the four creatures stay in the graveyard") {
                game.graveyardSize(1) shouldBe 4
            }
            withClue("CR 715.3d — the Adventure exiles itself, ready to be cast as the artifact") {
                game.isInExile(1, "Glamdring, Foe-hammer") shouldBe true
            }
        }
    }
}
