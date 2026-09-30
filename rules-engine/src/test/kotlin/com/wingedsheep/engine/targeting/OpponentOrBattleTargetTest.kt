package com.wingedsheep.engine.targeting

import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.legalactions.utils.TargetEnumerationUtils
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.engine.handlers.TargetingSourceType
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `TargetPermanentOrPlayer(opponentsOnly = true)` — "target opponent or battle" (Ayara, Widow of
 * the Realm). The player half must be the controller's *opponents* in the team-aware sense
 * (CR 102.3): in Two-Headed Giant the controller's teammate is not an opponent, so neither the
 * caster nor the teammate is a legal pick. Pinned on the three places that enumerate or check the
 * player half: the target finder, the legal-action enumerator and the validator.
 */
class OpponentOrBattleTargetTest : FunSpec({

    val evaluator = PredicateEvaluator(cardRegistry = null)

    /** Four seats in two teams of two, per CR 810.1: [[p1, p2], [p3, p4]]. */
    fun boot2hg() = GameInitializer(CardRegistry().apply { register(TestCards.all) }).initializeGame(
        GameConfig(
            format = Format.TwoHeadedGiant(),
            players = (1..4).map { PlayerConfig("Player $it", Deck.of("Forest" to 40)) },
            teams = listOf(listOf(0, 1), listOf(2, 3)),
            startingPlayerIndex = 0,
            skipMulligans = true,
        )
    ).state

    test("the finder and the enumerator offer only the opposing team") {
        val state = boot2hg()
        val (p1, _, p3, p4) = state.turnOrder

        TargetFinder(evaluator).findLegalTargets(state, Targets.OpponentOrBattle, p1)
            .shouldContainExactlyInAnyOrder(p3, p4)
        TargetEnumerationUtils(evaluator).findValidTargets(state, p1, Targets.OpponentOrBattle)
            .shouldContainExactlyInAnyOrder(p3, p4)
    }

    test("the validator rejects the caster and their teammate, accepts an opponent") {
        val state = boot2hg()
        val (p1, p2, p3, _) = state.turnOrder
        fun validate(player: com.wingedsheep.sdk.model.EntityId) = TargetValidator(evaluator).validateTargets(
            state = state,
            targets = listOf(ChosenTarget.Player(player)),
            requirements = listOf(Targets.OpponentOrBattle),
            casterId = p1,
            targetingSourceType = TargetingSourceType.ACTIVATED_ABILITY,
        )

        withClue("yourself") { validate(p1) shouldNotBe null }
        withClue("your Two-Headed Giant teammate") { validate(p2) shouldNotBe null }
        withClue("an opponent") { validate(p3) shouldBe null }
    }

    test("the plain player-or-battle shape still offers every player") {
        val state = boot2hg()
        TargetFinder(evaluator).findLegalTargets(state, Targets.PlayerOrBattle, state.turnOrder[0])
            .shouldContainExactlyInAnyOrder(state.turnOrder)
    }

    test("it reads as printed") {
        Targets.OpponentOrBattle.description shouldBe "target opponent or battle"
    }
})
