package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.DayNight
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Card-level scenario tests for the two VOW cards whose *own* text reaches into the day/night designation
 * — the code paths the generic [DayNightMechanicScenarioTest] doesn't touch because it drives
 * [com.wingedsheep.engine.mechanics.daynight.DayNightService] directly:
 *
 *  - **Into the Night** ({3}{R} sorcery) — "It becomes night. …" routes through the
 *    [com.wingedsheep.sdk.scripting.effects.SetDayNightEffect] executor (the effect layer), the third of
 *    the service's writers. This is the only test that a *spell* can set the designation.
 *  - **Wolf Strike** ({2}{G} instant) — "+2/+0 … if it's night" is the only exercise of
 *    [com.wingedsheep.sdk.dsl.Conditions.IsNight]: the buff (and therefore the damage, read from the
 *    pumped power) applies at night and not during day.
 *
 * Both cards are canonical VOW definitions loaded from the set catalog via [ScenarioTestBase]'s registry.
 */
class DayNightCardsScenarioTest : ScenarioTestBase() {

    init {
        context("Into the Night — 'It becomes night' (CR 731 via the effect executor)") {

            test("resolving Into the Night sets the designation to night") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Into the Night")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    // Discard fodder + a stocked library so the loot half can't deck anyone.
                    .withCardInHand(1, "Mountain")
                    .apply { repeat(6) { withCardInLibrary(1, "Mountain") } }
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("precondition: the game starts as neither day nor night (CR 731.1)") {
                    game.state.dayNight shouldBe null
                }

                game.castSpell(1, "Into the Night").error shouldBe null
                game.resolveStack()
                // The "discard any number" prompt auto-resolves to discarding zero (SelectCards → min).
                while (game.hasPendingDecision()) game.skipSelection()
                game.resolveStack()

                withClue("'It becomes night' set the designation via the SetDayNight executor") {
                    game.state.dayNight shouldBe DayNight.NIGHT
                }
            }
        }

        // Caster is a 2/2 Grizzly Bears (player 1); the target is a 3/3 Centaur Courser (player 2).
        // The 3-toughness target is the discriminator: 4 damage (2 base + night's +2/+0) kills it,
        // 2 damage (day / neither) leaves it alive — so target survival alone proves the condition.
        context("Wolf Strike — '+2/+0 … if it's night' (CR 731 / Conditions.IsNight)") {

            test("at night the buff applies: the 2/2 becomes a 4/4 and kills the 3/3") {
                val game = wolfStrikeBoard(dayNight = DayNight.NIGHT)
                val mine = game.findPermanent("Grizzly Bears")!!
                val theirs = game.findPermanent("Centaur Courser")!!
                val theirLifeBefore = game.getLifeTotal(2)

                castWolfStrike(game, mine, theirs)
                game.resolveStack()

                withClue("night → +2/+0 (until end of turn) applied, so the caster is now a 4/4") {
                    game.state.projectedState.getPower(mine) shouldBe 4
                }
                withClue("the +2/+0 landed before the damage, so the 4/4 dealt 4 — lethal to the 3/3") {
                    game.isOnBattlefield("Centaur Courser") shouldBe false
                    game.isInGraveyard(2, "Centaur Courser") shouldBe true
                }
                withClue("the caster survives (one-sided fight) and no damage hit the player") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.getLifeTotal(2) shouldBe theirLifeBefore
                }
            }

            test("during day the buff does NOT apply: the 2/2 deals only 2, the 3/3 survives") {
                val game = wolfStrikeBoard(dayNight = DayNight.DAY)
                val mine = game.findPermanent("Grizzly Bears")!!
                val theirs = game.findPermanent("Centaur Courser")!!

                castWolfStrike(game, mine, theirs)
                withClue("Conditions.IsNight is false during day, so no +2/+0 — the caster stays a 2/2") {
                    game.state.projectedState.getPower(mine) shouldBe 2
                }
                game.resolveStack()

                withClue("day → only 2 damage to the 3/3, which survives on the battlefield") {
                    game.isOnBattlefield("Centaur Courser") shouldBe true
                    game.isInGraveyard(2, "Centaur Courser") shouldBe false
                }
            }

            test("with no designation (neither) the buff does NOT apply and the 3/3 survives") {
                val game = wolfStrikeBoard(dayNight = null)
                withClue("precondition: neither day nor night (CR 731.1)") { game.state.dayNight shouldBe null }
                val mine = game.findPermanent("Grizzly Bears")!!
                val theirs = game.findPermanent("Centaur Courser")!!

                castWolfStrike(game, mine, theirs)
                withClue("IsNight is false when neither designation exists, so no +2/+0") {
                    game.state.projectedState.getPower(mine) shouldBe 2
                }
                game.resolveStack()

                withClue("neither → only 2 damage to the 3/3, which survives") {
                    game.isOnBattlefield("Centaur Courser") shouldBe true
                }
            }
        }
    }

    /**
     * A board with Wolf Strike in hand, {G} sources available, a 2/2 Grizzly Bears the caster controls,
     * a 3/3 Centaur Courser the opponent controls, and stocked libraries. [dayNight] is stamped straight
     * onto the built state.
     */
    private fun wolfStrikeBoard(dayNight: DayNight?): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Wolf Strike")
            .withLandsOnBattlefield(1, "Forest", 3)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Centaur Courser")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        if (dayNight != null) game.state = game.state.copy(dayNight = dayNight)
        return game
    }

    /** Cast Wolf Strike targeting [mine] (your creature, t1) then [theirs] (their creature, t2). */
    private fun castWolfStrike(game: TestGame, mine: EntityId, theirs: EntityId) {
        val cardId = game.state.getHand(game.player1Id).first { entityId ->
            game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Wolf Strike"
        }
        val cast = game.execute(
            CastSpell(
                game.player1Id,
                cardId,
                listOf(ChosenTarget.Permanent(mine), ChosenTarget.Permanent(theirs)),
            )
        )
        withClue("casting Wolf Strike should succeed: ${cast.error}") { cast.error shouldBe null }
    }
}
