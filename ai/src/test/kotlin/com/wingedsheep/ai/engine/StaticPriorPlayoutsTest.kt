package com.wingedsheep.ai.engine

import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [com.wingedsheep.ai.engine.rollout.RolloutSettings.staticPriorPlayouts], as [AiProfile.LIVE] ships
 * it.
 *
 * 2026-10-31 review of games against people, game `fccf5763` turn 12, rebuilt: at 11 life on an
 * empty board, facing a 2/1 and a 4/4 first striker, with five lands and four castable creatures in
 * hand, the AI passed both main phases and took 6. The static leaf preferred casting — Traxos,
 * Academy Guardian by 5 points — but sequential halving left each cast one playout, a lost one, and
 * the quarter-weight `logit` of that lone 0.0 outweighed it. Whether it passed came down to the RNG
 * seed, so the reproduction runs the position at several.
 */
class StaticPriorPlayoutsTest : ScenarioTestBase() {

    // Board-reading stat values ([CreatureValuation.statsReadTheBoard]) also tip this position to
    // casting, so the reproduction turns them off too: it pins what the static prior fixes on the
    // AI as it was.
    private val before = AiProfile.LIVE.copy(
        id = "live-without-static-prior",
        rollouts = AiProfile.LIVE.rollouts!!.copy(staticPriorPlayouts = 0.0),
        creatureValuation = AiProfile.LIVE.creatureValuation.copy(statsReadTheBoard = false),
    )

    private fun TestGame.choice(profile: AiProfile = AiProfile.LIVE): GameAction =
        AIPlayer.create(cardRegistry, player1Id, profile).chooseAction(state)

    private fun TestGame.castName(action: GameAction): String? =
        (action as? CastSpell)?.let { state.getEntity(it.cardId)?.get<CardComponent>()?.name }

    private fun position(seed: Long): TestGame {
        val builder = scenario().withPlayers()
            .withRngSeed(seed)
            .withTurnNumber(12)
            .withLifeTotal(1, 11)
            .withLifeTotal(2, 21)
            .withCardOnBattlefield(1, "Formidable Commons")
            .withCardOnBattlefield(1, "Transformative Commons")
            .withLandsOnBattlefield(1, "Forest", 2)
            .withLandsOnBattlefield(1, "Island", 1)
            .withCardInHand(1, "Cryotheory Adept")
            .withCardInHand(1, "Countersculpt")
            .withCardInHand(1, "Surveillance Phantasm")
            .withCardInHand(1, "Proft, Consulting Detective")
            .withCardInHand(1, "Ruric Thar, Biomagus")
            .withCardInHand(1, "Traxos, Academy Guardian")
            .withCardOnBattlefield(2, "Mindseeker Oculus")
            .withCardOnBattlefield(2, "Danitha, Spear of Agony")
            .withLandsOnBattlefield(2, "Swamp", 3)
            .withLandsOnBattlefield(2, "Island", 3)
            .withCardInHand(2, "Infinite Coursework")
            .withCardInHand(2, "Vraska's Final Mercy")
            .withCardInHand(2, "Island")
        listOf(
            "Sphinx of False Conclusions", "Null Summoner", "Recursive Recruitment", "Recursive Recruitment",
            "Fatehold Chronologist", "Last Gasp", "Tam's Resistance", "Extended Absence", "Protege's Awakening",
            "Silence the Echo", "Hapatra, the Desert Frost", "Way of the Deathbringer", "Theorix Charm",
            "Ferocity of the Hunt", "Swamp", "Swamp", "Swamp", "Island", "Island", "Swamp",
        ).forEach { builder.withCardInLibrary(2, it) }
        repeat(20) { builder.withCardInLibrary(1, if (it % 2 == 0) "Island" else "Forest") }
        val game = builder.build().advanceToPriority(1, Step.PRECOMBAT_MAIN)
        val danitha = game.findPermanent("Danitha, Spear of Agony")!!
        game.state = game.state.updateEntity(danitha) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withCounters(CounterType.PLUS_ONE_PLUS_ONE, 2))
        }
        return game
    }

    init {
        test("fccf5763 T12: the AI deploys a creature instead of passing with four castable") {
            // The seeds at which the profile without the prior passes (4, 16 and 24 when this was
            // found; which ones shifts with anything that moves the playouts' values, so they are
            // found rather than pinned). The reproduction needs at least one.
            val passing = (1L..24L).filter { seed -> position(seed).let { it.castName(it.choice(before)) == null } }
            passing.shouldNotBeEmpty()
            for (seed in passing) {
                val game = position(seed)
                game.castName(game.choice()) shouldNotBe null
            }
        }

        test("and at every other seed it casts too — the prior doesn't trade one pass for another") {
            for (seed in 1L..12L) {
                val game = position(seed)
                game.castName(game.choice()) shouldNotBe null
            }
        }
    }
}
