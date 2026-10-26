package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * `CostAtom.Unattach(ofGrantingPermanent = true)` (`Costs.UnattachGrantingPermanent`) — "Unattach
 * [this Equipment]" inside an ability the Equipment grants to the creature it equips (Toralf's
 * Hammer).
 *
 * Rules pinned here:
 *  - The cost detaches the *granting* Equipment, not the creature that has the ability: the host is
 *    attached to nothing, so the source-scoped `Costs.Unattach` would be unpayable for it.
 *  - Per CR 201.5a the name in the granted ability refers only to the specific granter, so a second
 *    Equipment on the same creature stays attached.
 *  - It is a cost (CR 118): paid on activation, before the ability resolves.
 */
class UnattachGrantingPermanentCostTest : ScenarioTestBase() {

    private fun rod(name: String, life: Int) = card(name) {
        manaCost = "{1}"
        typeLine = "Artifact — Equipment"
        staticAbility {
            ability = GrantActivatedAbility(
                ability = grantedActivatedAbility {
                    cost = Costs.UnattachGrantingPermanent
                    effect = Effects.GainLife(life)
                },
            )
        }
        equipAbility("{1}")
    }

    private val rodA = rod("Test Unattach Rod A", 1)
    private val rodB = rod("Test Unattach Rod B", 2)

    private fun grantedId(def: com.wingedsheep.sdk.model.CardDefinition) =
        def.script.staticAbilities.filterIsInstance<GrantActivatedAbility>().single().ability.id

    init {
        cardRegistry.register(listOf(rodA, rodB))

        fun board(): TestGame = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardAttachedTo(1, "Test Unattach Rod A", "Grizzly Bears")
            .withCardAttachedTo(1, "Test Unattach Rod B", "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("the granted ability is offered and affordable although its host is attached to nothing") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            val offered = game.getLegalActions(1).filter { info ->
                val action = info.action
                action is ActivateAbility && action.sourceId == bears && action.abilityId == grantedId(rodA)
            }
            offered.size shouldBe 1
            offered.single().isAffordable shouldBe true
        }

        test("paying it unattaches only the granting Equipment, as a cost") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            val a = game.findPermanent("Test Unattach Rod A")!!
            val b = game.findPermanent("Test Unattach Rod B")!!

            game.execute(ActivateAbility(game.player1Id, bears, grantedId(rodA))).error shouldBe null

            withClue("paid on activation: Rod A is off before the ability resolves") {
                game.state.getEntity(a)?.get<AttachedToComponent>() shouldBe null
                game.state.getEntity(bears)?.get<AttachmentsComponent>()?.attachedIds shouldBe listOf(b)
            }
            withClue("CR 201.5a — the other Equipment is untouched") {
                game.state.getEntity(b)?.get<AttachedToComponent>()?.targetId shouldBe bears
            }
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 21

            withClue("unequipped, the creature no longer has Rod A's ability") {
                game.getLegalActions(1).none { info ->
                    val action = info.action
                    action is ActivateAbility && action.sourceId == bears && action.abilityId == grantedId(rodA)
                } shouldBe true
            }
        }
    }
}
