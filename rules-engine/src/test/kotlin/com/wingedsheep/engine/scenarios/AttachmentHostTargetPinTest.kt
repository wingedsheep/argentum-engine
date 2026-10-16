package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * A target requirement phrased relative to the source's attachment host ("target creature that's
 * blocking equipped creature") is judged against the creature the source was attached to when the
 * target was chosen — at activation and again in the CR 608.2b resolution re-check — via
 * [TargetsComponent.targetingAttachmentHost]:
 *  - the Equipment moving to another creature, or becoming unattached, in response doesn't make the
 *    target illegal;
 *  - a target that stops blocking the originally equipped creature is illegal, and the ability
 *    doesn't resolve.
 */
class AttachmentHostTargetPinTest : ScenarioTestBase() {

    private val zapper = card("Host Pin Zapper") {
        manaCost = "{1}"
        typeLine = "Artifact — Equipment"
        activatedAbility {
            cost = Costs.Free
            val blocker = target(TargetFilter(GameObjectFilter.Creature.blockingEntity(EffectTarget.EquippedCreature)))
            effect = Effects.DealDamage(amount = 1, target = blocker, damageSource = EffectTarget.Self)
        }
        equipAbility("{2}")
    }

    private val bystander = card("Host Pin Bystander") {
        manaCost = "{1}"
        typeLine = "Creature — Human"
        power = 1; toughness = 1
    }

    init {
        cardRegistry.register(zapper)
        cardRegistry.register(bystander)
        val abilityId = zapper.activatedAbilities.first { !it.isEquipAbility }.id

        fun blockedAttack(): TestGame {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(1, "Host Pin Bystander", summoningSickness = false)
                .withCardAttachedTo(1, "Host Pin Zapper", "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null
            if (game.state.priorityPlayerId != game.player1Id) game.passPriority().error shouldBe null
            return game
        }

        fun activate(game: TestGame) {
            val giant = game.findPermanent("Hill Giant")!!
            game.execute(
                ActivateAbility(game.player1Id, game.findPermanent("Host Pin Zapper")!!, abilityId,
                    listOf(ChosenTarget.Permanent(giant)))
            ).error shouldBe null
        }

        fun giantDamage(game: TestGame): Int? =
            game.state.getEntity(game.findPermanent("Hill Giant")!!)?.get<DamageComponent>()?.amount

        context("target filters relative to the source's attachment host") {

            test("the host is captured with the targets") {
                val game = blockedAttack()
                activate(game)
                val stackObject = game.state.stack.last()
                game.state.getEntity(stackObject)?.get<TargetsComponent>()?.targetingAttachmentHost shouldBe
                    game.findPermanent("Grizzly Bears")
            }

            test("re-equipping onto another creature in response leaves the target legal") {
                val game = blockedAttack()
                activate(game)
                val zap = game.findPermanent("Host Pin Zapper")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val other = game.findPermanent("Host Pin Bystander")!!
                game.state = game.state
                    .updateEntity(zap) { it.with(AttachedToComponent(other)) }
                    .updateEntity(bears) { it.without<AttachmentsComponent>() }
                    .updateEntity(other) { it.with(AttachmentsComponent(listOf(zap))) }
                game.resolveStack()
                withClue("judged against Grizzly Bears, which Hill Giant still blocks") { giantDamage(game) shouldBe 1 }
            }

            test("becoming unattached in response leaves the target legal") {
                val game = blockedAttack()
                activate(game)
                val zap = game.findPermanent("Host Pin Zapper")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state = game.state
                    .updateEntity(zap) { it.without<AttachedToComponent>() }
                    .updateEntity(bears) { it.without<AttachmentsComponent>() }
                game.resolveStack()
                giantDamage(game) shouldBe 1
            }

            test("a target that stops blocking the pinned host is illegal and the ability doesn't resolve") {
                val game = blockedAttack()
                activate(game)
                val giant = game.findPermanent("Hill Giant")!!
                game.state = game.state.updateEntity(giant) { it.without<BlockingComponent>() }
                game.resolveStack()
                giantDamage(game) shouldBe null
            }
        }
    }
}
