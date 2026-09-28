package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Archangel Elspeth (MOM #6, {2}{W}{W}, loyalty 4).
 *
 *   +1: Create a 1/1 white Soldier creature token with lifelink.
 *   −2: Put two +1/+1 counters on target creature. It becomes an Angel in addition to its other
 *       types and gains flying.
 *   −6: Return all nonland permanent cards with mana value 3 or less from your graveyard to the
 *       battlefield.
 */
class ArchangelElspethScenarioTest : ScenarioTestBase() {

    init {
        val abilities = cardRegistry.getCard("Archangel Elspeth")!!.script.activatedAbilities
        fun idFor(change: Int) = abilities.single { (it.cost as? AbilityCost.Loyalty)?.change == change }.id

        fun base() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Archangel Elspeth")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        fun TestGame.loyalty(id: EntityId): Int =
            state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

        fun TestGame.activate(source: EntityId, change: Int, targets: List<EntityId> = emptyList()) {
            execute(
                ActivateAbility(
                    playerId = player1Id,
                    sourceId = source,
                    abilityId = idFor(change),
                    targets = targets.map { ChosenTarget.Permanent(it) }
                )
            ).error shouldBe null
            resolveStack()
        }

        test("+1 creates a 1/1 white Soldier token with lifelink") {
            val game = base().build()
            val elspeth = game.findPermanent("Archangel Elspeth")!!
            game.loyalty(elspeth) shouldBe 4

            game.activate(elspeth, +1)

            val token = game.state.getBattlefield().single { it != elspeth }
            val projected = game.state.projectedState
            projected.getPower(token) shouldBe 1
            projected.getToughness(token) shouldBe 1
            projected.hasSubtype(token, "Soldier") shouldBe true
            projected.hasKeyword(token, Keyword.LIFELINK) shouldBe true
            game.loyalty(elspeth) shouldBe 5
        }

        test("−2 puts two +1/+1 counters, makes it an Angel with flying, and it lasts past the turn") {
            val game = base()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .build()
            val elspeth = game.findPermanent("Archangel Elspeth")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.activate(elspeth, -2, listOf(bears))
            game.loyalty(elspeth) shouldBe 2

            fun check() {
                val projected = game.state.projectedState
                projected.getPower(bears) shouldBe 4
                projected.getToughness(bears) shouldBe 4
                projected.hasSubtype(bears, "Angel") shouldBe true
                projected.hasSubtype(bears, "Bear") shouldBe true
                projected.hasKeyword(bears, Keyword.FLYING) shouldBe true
            }
            withClue("right after resolution") { check() }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            withClue("the Angel type and flying have no duration") { check() }
        }

        test("−6 returns only your nonland permanent cards with mana value 3 or less") {
            val game = base()
                .withCardInGraveyard(1, "Grizzly Bears")     // creature, MV 2 -> returns
                .withCardInGraveyard(1, "Glorious Anthem")   // enchantment, MV 3 -> returns
                .withCardInGraveyard(1, "Hill Giant")        // creature, MV 4 -> stays
                .withCardInGraveyard(1, "Lightning Bolt")    // instant -> stays
                .withCardInGraveyard(1, "Forest")            // land -> stays
                .withCardInGraveyard(2, "Grizzly Bears")     // opponent's -> stays
                .build()
            val elspeth = game.findPermanent("Archangel Elspeth")!!
            game.state = game.state.updateEntity(elspeth) { c ->
                c.with(CountersComponent().withAdded(CounterType.LOYALTY, 6))
            }

            game.activate(elspeth, -6)

            withClue("MV <= 3 nonland permanents came back") {
                game.findPermanents("Grizzly Bears").size shouldBe 1
                game.findPermanent("Glorious Anthem") shouldBe game.findPermanents("Glorious Anthem").single()
            }
            withClue("everything else stayed in the graveyard") {
                game.findCardsInGraveyard(1, "Hill Giant").size shouldBe 1
                game.findCardsInGraveyard(1, "Lightning Bolt").size shouldBe 1
                game.findCardsInGraveyard(1, "Forest").size shouldBe 1
                game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 0
                game.findCardsInGraveyard(2, "Grizzly Bears").size shouldBe 1
            }
            withClue("Elspeth hit 0 loyalty and went to the graveyard") {
                game.findPermanent("Archangel Elspeth") shouldBe null
            }
        }
    }
}
