package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.player.FlashGrant
import com.wingedsheep.engine.state.components.player.FlashGrantsComponent
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `CouldCastSorcery` behind "each opponent can cast spells only any time they could cast a sorcery"
 * (Teferi, Time Raveler): `PlayersCantCastSpells(EachOpponent, Not(CouldCastSorcery),
 * conditionFromCaster = true)`.
 *
 * CR 307.5: "any time they could cast a sorcery" means the player has priority, it is the main
 * phase of their turn, and the stack is empty. Each clause gets a test — the turn, the phase, the
 * stack — plus the controller being unaffected, the lock outranking a flash permission (Teferi's
 * ruling), and the lock lifting with its source.
 */
class CouldCastSorceryRestrictionTest : FunSpec({

    val raveler = card("Sorcery-Speed Warden") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "Each opponent can cast spells only any time they could cast a sorcery."
        staticAbility {
            ability = PlayersCantCastSpells(
                affected = Player.EachOpponent,
                condition = Conditions.Not(Conditions.CouldCastSorcery),
                conditionFromCaster = true,
            )
        }
    }
    val instant = card("Warden Test Glint") { manaCost = "{G}"; typeLine = "Instant"; oracleText = "" }
    val sorcery = card("Warden Test Sorcery") { manaCost = "{G}"; typeLine = "Sorcery"; oracleText = "" }

    fun GameTestDriver.castActionsFor(playerId: EntityId, cardId: EntityId): List<CastSpell> =
        LegalActionEnumerator.create(cardRegistry)
            .enumerate(state, playerId)
            .mapNotNull { it.action as? CastSpell }
            .filter { it.cardId == cardId }

    fun driver(startingPlayer: Int) = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(raveler, instant, sorcery))
        initMirrorMatch(Deck.of("Forest" to 40), startingLife = 20, startingPlayer = startingPlayer)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("an opponent can't cast an instant during the controller's turn") {
        val d = driver(startingPlayer = 0)
        d.putPermanentOnBattlefield(d.player1, "Sorcery-Speed Warden")
        val spell = d.putCardInHand(d.player2, "Warden Test Glint")
        d.giveMana(d.player2, Color.GREEN, 1)

        d.castActionsFor(d.player2, spell) shouldHaveSize 0
        // The handler refuses it too, not just the enumerator.
        d.passPriority(d.player1)
        d.state.priorityPlayerId shouldBe d.player2
        d.castSpell(d.player2, spell).outcome shouldNotBe Outcome.Done
    }

    test("an opponent can cast an instant in their own main phase with an empty stack") {
        val d = driver(startingPlayer = 1)
        d.activePlayer shouldBe d.player2
        d.putPermanentOnBattlefield(d.player1, "Sorcery-Speed Warden")
        val spell = d.putCardInHand(d.player2, "Warden Test Glint")
        d.giveMana(d.player2, Color.GREEN, 1)

        d.castActionsFor(d.player2, spell).isNotEmpty() shouldBe true
        d.castSpell(d.player2, spell).outcome shouldBe Outcome.Done
    }

    test("an opponent can't cast an instant outside a main phase of their own turn") {
        val d = driver(startingPlayer = 1)
        d.putPermanentOnBattlefield(d.player1, "Sorcery-Speed Warden")
        val spell = d.putCardInHand(d.player2, "Warden Test Glint")
        d.passPriorityUntil(Step.END, d.player2)
        d.giveMana(d.player2, Color.GREEN, 1)

        d.castActionsFor(d.player2, spell) shouldHaveSize 0
    }

    test("an opponent can't respond while the stack is non-empty, even in their own main phase") {
        val d = driver(startingPlayer = 1)
        d.putPermanentOnBattlefield(d.player1, "Sorcery-Speed Warden")
        val first = d.putCardInHand(d.player2, "Warden Test Sorcery")
        val second = d.putCardInHand(d.player2, "Warden Test Glint")
        d.giveMana(d.player2, Color.GREEN, 2)

        d.castSpell(d.player2, first).outcome shouldBe Outcome.Done
        d.state.stack.isEmpty() shouldBe false
        d.state.priorityPlayerId shouldBe d.player2
        d.castActionsFor(d.player2, second) shouldHaveSize 0
    }

    test("the controller is unaffected and may cast instants on an opponent's turn") {
        val d = driver(startingPlayer = 1)
        d.putPermanentOnBattlefield(d.player1, "Sorcery-Speed Warden")
        val spell = d.putCardInHand(d.player1, "Warden Test Glint")
        d.passPriorityUntil(Step.END, d.player2)
        d.giveMana(d.player1, Color.GREEN, 1)

        d.castActionsFor(d.player1, spell).isNotEmpty() shouldBe true
    }

    test("the restriction outranks an opponent's flash permission") {
        val d = driver(startingPlayer = 0)
        d.putPermanentOnBattlefield(d.player1, "Sorcery-Speed Warden")
        d.replaceState(
            d.state.updateEntity(d.player2) {
                it.with(FlashGrantsComponent(listOf(FlashGrant(GameObjectFilter.Any, PlayerEffectRemoval.UntilYourNextTurn))))
            }
        )
        val spell = d.putCardInHand(d.player2, "Warden Test Sorcery")
        d.giveMana(d.player2, Color.GREEN, 1)

        d.castActionsFor(d.player2, spell) shouldHaveSize 0
    }

    test("the lock lifts when its source leaves the battlefield") {
        val d = driver(startingPlayer = 0)
        val warden = d.putPermanentOnBattlefield(d.player1, "Sorcery-Speed Warden")
        val spell = d.putCardInHand(d.player2, "Warden Test Glint")
        d.giveMana(d.player2, Color.GREEN, 1)
        d.castActionsFor(d.player2, spell) shouldHaveSize 0

        d.moveToGraveyard(warden)
        d.castActionsFor(d.player2, spell).isNotEmpty() shouldBe true
    }

    test("the static's text reads as printed") {
        (raveler.script.staticAbilities.single() as PlayersCantCastSpells).description shouldBe
            "Your opponents can cast spells only any time they could cast a sorcery"
    }
})
