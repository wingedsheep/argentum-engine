package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.puzzles.advanceToDeclaration
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [AiProfile.holdInstantSpeedDeploys], as [AiProfile.LIVE] ships it.
 *
 * The misplays are from the 2026-10-14 Reality Fracture draft games, each rebuilt as a minimal
 * position, with the answer the profile gave before the flag pinned beside it. The controls pin the
 * windows the held card is for, and the mana-contention release.
 */
class HoldInstantSpeedDeploysTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private val before = AiProfile.LIVE.copy(id = "live-without-deployhold", holdInstantSpeedDeploys = false)

    private fun TestGame.choice(profile: AiProfile = AiProfile.LIVE): GameAction =
        AIPlayer.create(cardRegistry, player1Id, profile).chooseAction(state)

    private fun TestGame.castName(action: GameAction): String? =
        (action as? CastSpell)?.let { state.getEntity(it.cardId)?.get<CardComponent>()?.name }

    init {

        // ── The card reading ──

        test("an attack-trigger loot is not a payoff on arrival") {
            val sphinx = intents.forName("Sphinx of False Conclusions")!!
            sphinx.tags shouldContain IntentTag.DRAW
            sphinx.tagsOnArrival shouldNotContain IntentTag.DRAW
        }

        test("Jace's Machinations lets its Jace act at instant speed") {
            intents.forName("Jace's Machinations")!!.grantsInstantSpeedLoyalty shouldBe true
            intents.forName("Tam's Resistance")!!.grantsInstantSpeedLoyalty shouldBe false
        }

        // ── Game 8522de5e, turn 8: Sphinx of False Conclusions ──

        test("T8: a flash flier whose only draw is on attacking is held in our own main phase") {
            val game = scenario().withPlayers()
                .withTurnNumber(8)
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInHand(1, "Sphinx of False Conclusions")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.castName(game.choice(before)) shouldBe "Sphinx of False Conclusions"
            game.castName(game.choice()) shouldBe null
        }

        test("the held flash flier comes down once the opponent has declared its attack") {
            val game = scenario().withPlayers()
                .withActivePlayer(2)
                .withTurnNumber(9)
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInHand(1, "Sphinx of False Conclusions")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()
                .advanceToDeclaration(2, Step.DECLARE_ATTACKERS)
                .also { it.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null }
                .advanceToPriority(1, Step.DECLARE_ATTACKERS)

            game.castName(game.choice()) shouldBe "Sphinx of False Conclusions"
        }

        test("a flash body is cast into idle mana when holding it would cost the other instant its mana") {
            // Five lands: the Sphinx and Murder cannot both be held up on the opponent's turn, so
            // the Sphinx spends this turn's mana and Murder stays up.
            val game = scenario().withPlayers()
                .withTurnNumber(8)
                .withLandsOnBattlefield(1, "Island", 3)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInHand(1, "Sphinx of False Conclusions")
                .withCardInHand(1, "Murder")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.castName(game.choice()) shouldBe "Sphinx of False Conclusions"
        }

        test("with mana for both on the opponent's turn, the flash body is still held") {
            val game = scenario().withPlayers()
                .withTurnNumber(8)
                .withLandsOnBattlefield(1, "Island", 4)
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInHand(1, "Sphinx of False Conclusions")
                .withCardInHand(1, "Murder")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.castName(game.choice()) shouldNotBe "Sphinx of False Conclusions"
        }

        // ── Game 2e28f192, turn 6: Jace's Machinations ──

        test("T6: an instant whose Jace it lets act at instant speed is held in our own main phase") {
            // Seeded: with the Jace token priced at what its loyalty buys
            // ([AiProfile.walkersPricedAsEngines]), the profile without the hold scores the cast
            // and passing within rollout noise of each other here, so an entropy-seeded build
            // flipped `before`'s answer from run to run. The hold's own answer never moved — the
            // floor puts the cast below passing whatever the rollouts say.
            val game = scenario().withPlayers()
                .withRngSeed(1L)
                .withTurnNumber(6)
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInHand(1, "Jace's Machinations")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.castName(game.choice(before)) shouldBe "Jace's Machinations"
            game.castName(game.choice()) shouldBe null
        }

        test("the held Machinations is cast at the opponent's end step") {
            val game = scenario().withPlayers()
                .withActivePlayer(2)
                .withTurnNumber(7)
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInHand(1, "Jace's Machinations")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()
                .advanceToPriority(1, Step.END)

            game.castName(game.choice()) shouldBe "Jace's Machinations"
        }

        test("a token instant with no-haste bodies waits for the opponent's turn") {
            val game = scenario().withPlayers()
                .withTurnNumber(4)
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInHand(1, "Raise the Alarm")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.castName(game.choice()) shouldBe null
        }
    }
}
