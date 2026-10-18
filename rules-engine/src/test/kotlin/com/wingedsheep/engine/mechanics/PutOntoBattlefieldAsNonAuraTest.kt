package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.chosenPlayer
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * [com.wingedsheep.sdk.scripting.effects.PutOntoBattlefieldAsNonAuraEffect] — an Aura card returns
 * as a non-Aura enchantment that keeps its other abilities, gains new ones and remembers a player
 * as `Player.ChosenPlayer` — and the `chooser` axis of
 * [com.wingedsheep.sdk.scripting.effects.PutOntoBattlefieldAttachedToChosenEffect] (Takklemaggot).
 */
class PutOntoBattlefieldAsNonAuraTest : FunSpec({
    val maggot = card("Test NonAura Maggot") {
        manaCost = "{B}"
        typeLine = "Enchantment — Aura"
        auraTarget = TargetObject(filter = TargetFilter.Creature)
        // A printed ability that keeps working once it is no Aura.
        staticAbility { ability = ModifyStats(1, 0, GroupFilter(GameObjectFilter.Creature.youControl())) }
        // An enchanted-creature-timed step trigger: inert once unattached, even beside the granted one.
        triggeredAbility {
            trigger = Triggers.attached.beginningOf(Step.UPKEEP)
            effect = Effects.GainLife(5)
        }
        triggeredAbility {
            trigger = Triggers.attached.dies()
            effect = Effects.PutOntoBattlefieldAsNonAura(
                grantedTriggeredAbilities = listOf(
                    TriggeredAbility.create(
                        trigger = Triggers.player(Player.ChosenPlayer).beginningOf(Step.UPKEEP),
                        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.TriggeringPlayer)),
                    )
                ),
                chosenPlayer = EffectTarget.ControllerOfTriggeringEntity,
            )
        }
    }
    val hopper = card("Test Opponent Picks Hopper") {
        manaCost = "{B}"
        typeLine = "Enchantment — Aura"
        auraTarget = TargetObject(filter = TargetFilter.Creature)
        triggeredAbility {
            trigger = Triggers.attached.dies()
            effect = Effects.PutOntoBattlefieldAttachedToChosen(
                EffectTarget.Self, GameObjectFilter.Creature, chooser = Chooser.Opponent
            )
        }
    }
    val kill = card("Test NonAura Kill") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.Destroy(creature)
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(maggot, hopper, kill))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.attach(auraId: EntityId, hostId: EntityId) {
        addComponent(auraId, AttachedToComponent(hostId))
        addComponent(hostId, AttachmentsComponent(listOf(auraId)))
    }

    fun GameTestDriver.destroy(target: EntityId) {
        val p = activePlayer!!
        castSpellWithTargets(p, putCardInHand(p, "Test NonAura Kill"), listOf(ChosenTarget.Permanent(target))).error shouldBe null
        repeat(6) { if (state.pendingDecision == null && state.stack.isNotEmpty()) bothPass() }
    }

    test("returns unattached and no longer an Aura, keeps its printed abilities, survives SBAs, records the player") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val victim = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val mine = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val aura = d.putPermanentOnBattlefield(me, "Test NonAura Maggot")
        d.attach(aura, victim)

        d.destroy(victim)

        d.state.getBattlefield().contains(aura) shouldBe true
        val projected = d.state.projectedState
        projected.hasSubtype(aura, "Aura") shouldBe false
        projected.hasType(aura, "ENCHANTMENT") shouldBe true
        projected.getController(aura) shouldBe me
        d.state.getEntity(aura)?.get<AttachedToComponent>() shouldBe null
        d.state.getEntity(aura)?.chosenPlayer() shouldBe opp
        projected.getPower(mine) shouldBe 3
    }

    test("the granted step trigger fires on the chosen player's upkeep only and hurts that player") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val victim = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val aura = d.putPermanentOnBattlefield(me, "Test NonAura Maggot")
        d.attach(aura, victim)
        d.destroy(victim)

        d.passPriorityUntil(Step.END)
        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe opp
        d.state.stack.size shouldBe 1
        // Gone in response, the ping still finds "that player" — the step's active player.
        d.moveToGraveyard(aura)
        d.bothPass()
        d.getLifeTotal(opp) shouldBe 19
        d.getLifeTotal(me) shouldBe 20

    }

    test("neither the granted trigger nor the printed attached-timed one fires on its controller's upkeep") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val victim = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val aura = d.putPermanentOnBattlefield(me, "Test NonAura Maggot")
        d.attach(aura, victim)
        d.destroy(victim)

        d.passPriorityUntil(Step.END)
        d.passPriorityUntil(Step.UPKEEP)
        d.bothPass()
        d.passPriorityUntil(Step.END)
        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe me
        d.state.stack.size shouldBe 0
        d.getLifeTotal(me) shouldBe 20
    }

    test("the chooser axis hands the host choice to another player; the Aura stays under its controller") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val victim = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val other = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val theirs = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val aura = d.putPermanentOnBattlefield(me, "Test Opponent Picks Hopper")
        d.attach(aura, victim)

        d.destroy(victim)

        val decision = d.pendingDecision
        decision.shouldBeInstanceOf<ChooseTargetsDecision>()
        decision.playerId shouldBe opp
        decision.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(other, theirs)
        d.submitTargetSelection(opp, listOf(theirs))

        d.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId shouldBe theirs
        d.state.projectedState.getController(aura) shouldBe me
    }
})
