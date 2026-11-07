package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Nahiri's Wrath (EMN #137) — {2}{R} Sorcery.
 *
 * "As an additional cost to cast this spell, discard X cards.
 *  Nahiri's Wrath deals damage equal to the total mana value of the discarded cards to each of up to
 *  X target creatures and/or planeswalkers."
 *
 * X is announced with the spell (CR 601.2b) and sizes the targets; each target takes the full total
 * — the damage isn't divided.
 */
class NahirisWrathScenarioTest : ScenarioTestBase() {

    private fun TestGame.card(name: String): EntityId =
        state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun TestGame.cast(x: Int, discarded: List<String>, targets: List<EntityId>) = execute(
        CastSpell(
            player1Id,
            card("Nahiri's Wrath"),
            targets.map { ChosenTarget.Permanent(it) },
            xValue = x,
            additionalCostPayment = AdditionalCostPayment(discardedCards = discarded.map { card(it) }),
        )
    )

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Nahiri's Wrath")
        .withCardInHand(1, "Grizzly Bears") // mana value 2
        .withCardInHand(1, "Hill Giant") // mana value 4
        .withLandsOnBattlefield(1, "Mountain", 3)
        .withCardOnBattlefield(2, "Serra Angel") // 4/4
        .withCardOnBattlefield(2, "Jace Beleren") // loyalty 3
        .withCardOnBattlefield(2, "Hill Giant") // 3/3
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("discarding two cards deals their total mana value to each of two targets") {
            val game = board()
            val angel = game.findPermanent("Serra Angel")!!
            val jace = game.findPermanent("Jace Beleren")!!

            val cast = game.cast(x = 2, discarded = listOf("Grizzly Bears", "Hill Giant"), targets = listOf(angel, jace))
            withClue("cast: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            withClue("6 damage to the 4/4 angel") { game.isOnBattlefield("Serra Angel") shouldBe false }
            withClue("6 damage to the planeswalker") { game.isOnBattlefield("Jace Beleren") shouldBe false }
            withClue("the untargeted giant is untouched") { game.findPermanent("Hill Giant") shouldNotBe null }
        }

        test("up to X: two discards may pick a single target") {
            val game = board()
            val angel = game.findPermanent("Serra Angel")!!

            val cast = game.cast(x = 2, discarded = listOf("Grizzly Bears", "Hill Giant"), targets = listOf(angel))
            withClue("cast: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            withClue("6 damage kills the 4/4") { game.isOnBattlefield("Serra Angel") shouldBe false }
            game.isOnBattlefield("Jace Beleren") shouldBe true
        }

        test("one discard allows only one target and deals that card's mana value") {
            val game = board()
            val angel = game.findPermanent("Serra Angel")!!
            val giant = game.findPermanents("Hill Giant").single()

            withClue("two targets with X = 1 is illegal") {
                game.cast(x = 1, discarded = listOf("Grizzly Bears"), targets = listOf(angel, giant)).error shouldNotBe null
            }
            val cast = game.cast(x = 1, discarded = listOf("Grizzly Bears"), targets = listOf(giant))
            withClue("cast: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()
            withClue("2 damage doesn't kill a 3/3") { game.findPermanents("Hill Giant").size shouldBe 1 }
            game.isOnBattlefield("Serra Angel") shouldBe true
        }
    }
}
