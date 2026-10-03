package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Bloodsoaked Insight // Sanguine Morass (MH3).
 *
 * Front: "This spell costs {1} less to cast for each 1 life your opponents have lost this turn.
 * Target opponent exiles the top three cards of their library. Until the end of your next turn, you
 * may play those cards. If you cast a spell this way, mana of any type can be spent to cast it."
 * Back: "This land enters tapped. {T}: Add {B} or {R}."
 */
class BloodsoakedInsightScenarioTest : ScenarioTestBase() {

    private fun board(mountains: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Bloodsoaked Insight")
        .withCardInHand(1, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Mountain", mountains)
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Grizzly Bears")
        .withCardInLibrary(2, "Forest")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("after Bolting the opponent it costs {3} less; exiles their top three, castable with any mana") {
            val game = board(mountains = 7)

            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 17

            // {5}{B/R}{B/R} minus 3 = four Mountains.
            game.castSpellTargetingPlayer(1, "Bloodsoaked Insight", 2).error shouldBe null
            game.resolveStack()

            val untappedMountains = game.state.getBattlefield().count { id ->
                val e = game.state.getEntity(id)!!
                e.get<CardComponent>()!!.name == "Mountain" && !e.has<TappedComponent>()
            }
            untappedMountains shouldBe 2

            game.state.getLibrary(game.player2Id).size shouldBe 0
            val exiled = game.state.getExile(game.player2Id)
            exiled.size shouldBe 3
            exiled.forEach { id -> game.state.mayPlayPermissions.any { id in it.cardIds } shouldBe true }

            // Two Mountains left: the green Grizzly Bears is paid with red mana.
            // The Bears sit in the opponent's exile, so cast by id rather than via castSpellFromExile.
            val bears = exiled.first { id -> game.state.getEntity(id)!!.get<CardComponent>()!!.name == "Grizzly Bears" }
            game.execute(CastSpell(game.player1Id, bears)).error shouldBe null
            game.resolveStack()
            game.findPermanent("Grizzly Bears") shouldNotBe null
        }

        test("with no life lost this turn, six Mountains can't pay the full {5}{B/R}{B/R}") {
            val game = board(mountains = 6)
            game.castSpellTargetingPlayer(1, "Bloodsoaked Insight", 2).error shouldNotBe null
        }

        test("with no life lost this turn, seven Mountains pay the full cost") {
            val game = board(mountains = 7)
            game.castSpellTargetingPlayer(1, "Bloodsoaked Insight", 2).error shouldBe null
        }

        test("played as Sanguine Morass, it enters tapped") {
            val game = board(mountains = 0)
            val card = game.state.getHand(game.player1Id).first { id ->
                game.state.getEntity(id)!!.get<CardComponent>()!!.name == "Bloodsoaked Insight"
            }
            game.execute(PlayLand(game.player1Id, card, asBackFace = true)).error shouldBe null

            val land = game.findPermanent("Sanguine Morass")!!
            game.state.getEntity(land)!!.has<TappedComponent>() shouldBe true
        }
    }
}
