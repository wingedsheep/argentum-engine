package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.thb.cards.WingsOfHubris
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Wings of Hubris (THB #241).
 *
 * "Equipped creature has flying. / Sacrifice this Equipment: Equipped creature can't be blocked this
 * turn. Sacrifice it at the beginning of the next end step. / Equip {1}"
 *
 * The Equipment is gone by the time its ability resolves, so the pins are that "equipped creature"
 * still means the creature it was attached to (it becomes unblockable), and that the delayed
 * end-step sacrifice tracks that creature rather than re-reading an attachment that no longer exists.
 */
class WingsOfHubrisScenarioTest : FunSpec({

    val projector = StateProjector()
    val equipId = WingsOfHubris.activatedAbilities.single { it.isEquipAbility }.id
    val sacrificeId = WingsOfHubris.activatedAbilities.single { !it.isEquipAbility }.id

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20, skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.equip(player: EntityId, wings: EntityId, creature: EntityId) {
        giveColorlessMana(player, 1)
        submit(
            ActivateAbility(
                playerId = player,
                sourceId = wings,
                abilityId = equipId,
                targets = listOf(ChosenTarget.Permanent(creature))
            )
        ).outcome shouldBe Outcome.Done
        bothPass()
        state.getEntity(wings)?.get<AttachedToComponent>()?.targetId shouldBe creature
    }

    fun GameTestDriver.sacrificeWings(player: EntityId, wings: EntityId) {
        val result = submit(ActivateAbility(playerId = player, sourceId = wings, abilityId = sacrificeId))
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        findPermanent(player, "Wings of Hubris") shouldBe null
        bothPass()
    }

    test("equipped creature has flying") {
        val d = setup()
        val you = d.activePlayer!!
        val bear = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        val wings = d.putPermanentOnBattlefield(you, "Wings of Hubris")

        projector.project(d.state).hasKeyword(bear, Keyword.FLYING) shouldBe false
        d.equip(you, wings, bear)
        projector.project(d.state).hasKeyword(bear, Keyword.FLYING) shouldBe true
    }

    test("sacrificing it makes the former bearer unblockable, then sacrifices that creature at end step") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        val bear = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        d.removeSummoningSickness(bear)
        val bystander = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        val birds = d.putCreatureOnBattlefield(opponent, "Birds of Paradise")
        d.removeSummoningSickness(birds)
        val wings = d.putPermanentOnBattlefield(you, "Wings of Hubris")
        d.equip(you, wings, bear)

        d.sacrificeWings(you, wings)
        d.getGraveyard(you) shouldContain wings
        withClue("the flying grant left with the Equipment") {
            projector.project(d.state).hasKeyword(bear, Keyword.FLYING) shouldBe false
        }

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(you, listOf(bear), opponent).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        withClue("the former bearer can't be blocked this turn") {
            (d.declareBlockers(opponent, mapOf(birds to listOf(bear))).error != null) shouldBe true
        }
        d.declareBlockers(opponent, emptyMap()).error shouldBe null

        d.passPriorityUntil(Step.END)
        var guard = 0
        while (guard++ < 10 && (d.state.stack.isNotEmpty() || d.pendingDecision != null)) {
            if (d.pendingDecision != null) d.autoResolveDecision() else d.bothPass()
        }

        withClue("the creature that wore the Wings is sacrificed at the next end step") {
            d.getGraveyard(you) shouldContain bear
            d.state.getBattlefield().contains(bear) shouldBe false
        }
        withClue("only that creature is sacrificed") {
            d.state.getBattlefield().contains(bystander) shouldBe true
        }
    }

    test("sacrificing it while unattached does nothing beyond the sacrifice") {
        val d = setup()
        val you = d.activePlayer!!
        val bear = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        val wings = d.putPermanentOnBattlefield(you, "Wings of Hubris")

        d.sacrificeWings(you, wings)
        d.getGraveyard(you) shouldContain wings

        d.passPriorityUntil(Step.END)
        d.stackSize shouldBe 0
        d.findPermanent(you, "Grizzly Bears") shouldNotBe null
        d.state.getBattlefield().contains(bear) shouldBe true
    }
})
