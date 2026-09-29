package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mom.cards.ScornBladeBerserker
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.GrantActivatedAbilityEffect
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Scorn-Blade Berserker (MOM #124) — backup 1 and "{1}, Sacrifice this creature: Draw a card". */
class ScornBladeBerserkerScenarioTest : ScenarioTestBase() {
    private val ability = ScornBladeBerserker.activatedAbilities.single().id
    private val grantedAbility = (
        ((ScornBladeBerserker.triggeredAbilities.single().effect as CompositeEffect).effects[1] as GatedEffect).then
            as GrantActivatedAbilityEffect
        ).ability.id

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun castBerserker(): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Scorn-Blade Berserker")
            .withLandsOnBattlefield(1, "Swamp", 3)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(1, "Scorn-Blade Berserker").error shouldBe null
        game.resolveStack()
        game.hasPendingDecision() shouldBe true
        return game
    }

    init {
        test("backup on another creature: a counter and it can sacrifice itself to draw") {
            val game = castBerserker()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()
            plusOnes(game, bears) shouldBe 1
            val handBefore = game.handSize(1)
            game.execute(ActivateAbility(game.player1Id, bears, grantedAbility, emptyList())).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.handSize(1) shouldBe handBefore + 1
        }

        test("backup on itself: the counter only, Grizzly Bears gains no ability") {
            val game = castBerserker()
            val berserker = game.findPermanent("Scorn-Blade Berserker")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            game.selectTargets(listOf(berserker)).error shouldBe null
            game.resolveStack()
            plusOnes(game, berserker) shouldBe 1
            game.state.projectedState.getPower(berserker) shouldBe 1
            game.execute(ActivateAbility(game.player1Id, bears, grantedAbility, emptyList())).error shouldNotBe null
        }

        test("its own ability: {1}, sacrifice it, draw a card") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Scorn-Blade Berserker")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val berserker = game.findPermanent("Scorn-Blade Berserker")!!
            val handBefore = game.handSize(1)
            game.execute(ActivateAbility(game.player1Id, berserker, ability, emptyList())).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Scorn-Blade Berserker") shouldBe true
            game.handSize(1) shouldBe handBefore + 1
        }
    }
}
