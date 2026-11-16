package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Living Lore (DTK #61) — {3}{U} Creature — Avatar, star/star.
 *
 * "As this creature enters, exile an instant or sorcery card from your graveyard.
 *  Living Lore's power and toughness are each equal to the exiled card's mana value.
 *  Whenever this creature deals combat damage, you may sacrifice it. If you do, you may cast the
 *  exiled card without paying its mana cost."
 *
 * Pins the linked set: the as-enters exile links the card to this permanent, the CDA reads that
 * card's mana value, and the post-sacrifice cast still finds it — on the opponent's turn, which is
 * also the ruling that a sorcery can be cast this way at instant speed.
 */
class LivingLoreScenarioTest : ScenarioTestBase() {

    /** Casts Living Lore from hand and, if asked, exiles [exile] as it enters. */
    private fun TestGame.castLivingLore(exile: String?) {
        execute(
            CastSpell(playerId = player1Id, cardId = findCardsInHand(1, "Living Lore").single())
        ).error shouldBe null
        resolveStack()
        val selection = state.pendingDecision
        if (selection is SelectCardsDecision && exile != null) {
            selectCards(listOf(findCardsInGraveyard(1, exile).single()))
        }
    }

    /**
     * Living Lore (exiling Divination, so a 3/3) is cast in Alice's second main phase; on Bob's
     * turn his Grizzly Bears attack and Living Lore blocks, dealing combat damage.
     */
    private fun TestGame.blockOnBobsTurn() {
        castLivingLore("Divination")
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        declareBlockers(mapOf("Living Lore" to listOf("Grizzly Bears"))).error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        if (state.pendingDecision != null) submitDefaultCombatDamage()
        resolveStack()
    }

    private fun combatBoard() = scenario()
        .withPlayers("Alice", "Bob")
        .withCardInHand(1, "Living Lore")
        .withLandsOnBattlefield(1, "Island", 4)
        .withCardInGraveyard(1, "Divination")
        .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
        .build()

    init {
        test("exiles the chosen instant or sorcery card and takes its mana value as its P/T") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Living Lore")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInGraveyard(1, "Divination")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.execute(
                CastSpell(playerId = game.player1Id, cardId = game.findCardsInHand(1, "Living Lore").single())
            ).error shouldBe null
            game.resolveStack()

            val selection = game.state.pendingDecision
            selection.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("only the instant and the sorcery are offered, not the creature card") {
                selection.options.toSet() shouldBe setOf(
                    game.findCardsInGraveyard(1, "Divination").single(),
                    game.findCardsInGraveyard(1, "Lightning Bolt").single(),
                )
            }
            game.selectCards(listOf(game.findCardsInGraveyard(1, "Divination").single()))

            val lore = game.findPermanent("Living Lore").shouldNotBeNull()
            withClue("Divination has mana value 3") {
                game.state.projectedState.getPower(lore) shouldBe 3
                game.state.projectedState.getToughness(lore) shouldBe 3
            }
            game.isInExile(1, "Divination") shouldBe true
            game.isInGraveyard(1, "Lightning Bolt") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
        }

        test("with no instant or sorcery card in the graveyard it is a 0/0 and dies") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Living Lore")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInGraveyard(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castLivingLore(exile = null)

            game.findPermanent("Living Lore") shouldBe null
            game.isInGraveyard(1, "Living Lore") shouldBe true
            withClue("the creature card was never eligible") {
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }
        }

        test("sacrificing it after combat damage casts the exiled sorcery for free on the opponent's turn") {
            val game = combatBoard()
            game.blockOnBobsTurn()
            val handBefore = game.state.getZone(ZoneKey(game.player1Id, Zone.HAND)).size

            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true)

            withClue("Living Lore was sacrificed") {
                game.findPermanent("Living Lore") shouldBe null
                game.isInGraveyard(1, "Living Lore") shouldBe true
            }
            val castChoice = game.state.pendingDecision
            castChoice.shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(listOf(castChoice.options.single()))
            game.resolveStack()

            // Alice's four Islands are still tapped from casting Living Lore on her own turn, so
            // Divination resolving at all means it was cast without paying its mana cost.
            withClue("Divination resolved on Bob's turn and drew two cards") {
                game.state.activePlayerId shouldBe game.player2Id
                game.state.getZone(ZoneKey(game.player1Id, Zone.HAND)).size shouldBe handBefore + 2
                game.isInGraveyard(1, "Divination") shouldBe true
            }
        }

        test("declining the sacrifice keeps Living Lore and leaves the card exiled") {
            val game = combatBoard()
            game.blockOnBobsTurn()

            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false)

            val lore = game.findPermanent("Living Lore").shouldNotBeNull()
            game.state.projectedState.getPower(lore) shouldBe 3
            game.isInExile(1, "Divination") shouldBe true
        }
    }
}
