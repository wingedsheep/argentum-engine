package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * [com.wingedsheep.sdk.scripting.effects.PutOntoBattlefieldAsAuraEffect]: a dying creature card
 * returns as an Aura it isn't printed as — "It's an Aura enchantment with enchant Forest you control
 * and '…', and it loses all other abilities" (Harold and Bob, First Numens).
 */
class PutOntoBattlefieldAsAuraTest : FunSpec({
    val grantedTap = ActivatedAbility(
        id = AbilityId("test-aura-numen-granted-tap"), cost = Costs.Tap, effect = Effects.AddMana(Color.RED, 3),
        isManaAbility = true, timing = TimingRule.ManaAbility
    )
    val numen = card("Test Aura Numen") {
        manaCost = "{2}{G}"
        typeLine = "Legendary Creature — Treefolk"
        power = 3
        toughness = 3
        keywords(Keyword.REACH)
        staticAbility {
            ability = ModifyStats(1, 1, GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true))
        }
        triggeredAbility {
            trigger = Triggers.self.dies()
            effect = Effects.PutOntoBattlefieldAsAura(
                enchant = GameObjectFilter.Land.withSubtype(Subtype.FOREST).youControl(),
                grantedStaticAbilities = listOf(GrantActivatedAbility(grantedTap))
            )
        }
    }
    val lionPump = ActivatedAbility(
        id = AbilityId("test-aura-lion-granted-gain"), cost = Costs.Tap, effect = Effects.GainLife(1)
    )
    val lion = card("Test Aura Lion") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Cat"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.self.dies()
            effect = Effects.PutOntoBattlefieldAsAura(
                enchant = GameObjectFilter.Creature.youControl(),
                grantedStaticAbilities = listOf(
                    GrantKeyword(Keyword.FLYING, GroupFilter.attachedCreature()),
                    ModifyStats(1, 1, GroupFilter.attachedCreature())
                ),
                grantedAbilities = listOf(lionPump)
            )
        }
    }
    val kill = card("Test Aura Kill") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.Destroy(creature)
        }
    }
    val razeForest = card("Test Aura Raze") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val land = target(TargetFilter.Land)
            effect = Effects.Destroy(land)
        }
    }
    val watcher = card("Test Aura Watcher") {
        manaCost = "{W}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.another(GameObjectFilter.Creature).enters()
            effect = Effects.GainLife(1)
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(numen, lion, kill, razeForest, watcher))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Destroy [target] with a free instant and pass until the stack is empty or a decision is pending. */
    fun GameTestDriver.destroy(spell: String, target: EntityId) {
        val p = activePlayer!!
        castSpellWithTargets(p, putCardInHand(p, spell), listOf(ChosenTarget.Permanent(target))).error shouldBe null
        repeat(6) { if (state.pendingDecision == null && state.stack.isNotEmpty()) bothPass() }
    }

    test("dies and returns as an Aura on the chosen Forest you control, with nothing else") {
        val d = driver()
        val p = d.activePlayer!!
        val opp = d.getOpponent(p)
        val numenId = d.putCreatureOnBattlefield(p, numen.name)
        val ally = d.putCreatureOnBattlefield(p, "Grizzly Bears")
        val forestA = d.putLandOnBattlefield(p, "Forest")
        val forestB = d.putLandOnBattlefield(p, "Forest")
        d.putLandOnBattlefield(opp, "Forest")
        d.putLandOnBattlefield(p, "Mountain")
        d.state.projectedState.getPower(ally) shouldBe 3

        d.destroy(kill.name, numenId)

        // CR 303.4f: chosen as it enters, among Forests *you* control only.
        val decision = d.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        decision.playerId shouldBe p
        decision.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(forestA, forestB)
        d.submitTargetSelection(p, listOf(forestB)).error shouldBe null

        val projected = d.state.projectedState
        (numenId in d.state.getBattlefield()) shouldBe true
        d.state.getEntity(numenId)!!.get<AttachedToComponent>()!!.targetId shouldBe forestB
        projected.hasType(numenId, "ENCHANTMENT") shouldBe true
        projected.isCreature(numenId) shouldBe false
        projected.getSubtypes(numenId) shouldBe setOf("Aura")
        projected.getSupertypes(numenId) shouldBe setOf("LEGENDARY")
        projected.hasKeyword(numenId, Keyword.REACH) shouldBe false
        // Its printed anthem is gone with its other abilities.
        projected.getPower(ally) shouldBe 2
        projected.getController(numenId) shouldBe p
    }

    test("the enchanted Forest has the granted ability, and the Aura's own lost abilities stay lost") {
        val d = driver()
        val p = d.activePlayer!!
        val numenId = d.putCreatureOnBattlefield(p, numen.name)
        val forest = d.putLandOnBattlefield(p, "Forest")
        d.destroy(kill.name, numenId)
        d.submitTargetSelection(p, listOf(forest)).error shouldBe null

        val activations = d.legalActions(p).mapNotNull { it.action as? ActivateAbility }
        activations.any { it.sourceId == forest && it.abilityId == grantedTap.id } shouldBe true
        activations.none { it.sourceId == numenId } shouldBe true
        d.submit(ActivateAbility(p, forest, grantedTap.id)).error shouldBe null
        d.state.getEntity(p)!!.get<ManaPoolComponent>()!!.red shouldBe 3
    }

    test("a creature-entering observer doesn't see it: it is never a creature on the battlefield") {
        val d = driver()
        val p = d.activePlayer!!
        val numenId = d.putCreatureOnBattlefield(p, numen.name)
        val forest = d.putLandOnBattlefield(p, "Forest")
        d.putCreatureOnBattlefield(p, watcher.name)
        val life = d.getLifeTotal(p)
        d.destroy(kill.name, numenId)
        d.submitTargetSelection(p, listOf(forest)).error shouldBe null
        repeat(4) { if (d.state.stack.isNotEmpty()) d.bothPass() }
        d.getLifeTotal(p) shouldBe life
    }

    test("with no Forest to enchant it stays in the graveyard (CR 303.4g)") {
        val d = driver()
        val p = d.activePlayer!!
        val opp = d.getOpponent(p)
        val numenId = d.putCreatureOnBattlefield(p, numen.name)
        d.putLandOnBattlefield(opp, "Forest")
        d.destroy(kill.name, numenId)
        d.state.pendingDecision shouldBe null
        (numenId in d.getGraveyard(p)) shouldBe true
        (numenId in d.state.getBattlefield()) shouldBe false
    }

    test("when its Forest leaves, the Aura is put into the graveyard and doesn't come back (CR 704.5m)") {
        val d = driver()
        val p = d.activePlayer!!
        val numenId = d.putCreatureOnBattlefield(p, numen.name)
        val forest = d.putLandOnBattlefield(p, "Forest")
        d.putLandOnBattlefield(p, "Forest")
        d.destroy(kill.name, numenId)
        d.submitTargetSelection(p, listOf(forest)).error shouldBe null

        d.destroy(razeForest.name, forest)
        d.state.pendingDecision shouldBe null
        (numenId in d.getGraveyard(p)) shouldBe true
    }
    test("granted keyword and P/T statics outlast the Aura's own ability loss (CR 613.7a), and granted activated abilities work") {
        val d = driver()
        val p = d.activePlayer!!
        val lionId = d.putCreatureOnBattlefield(p, lion.name)
        val bear = d.putCreatureOnBattlefield(p, "Grizzly Bears")
        d.destroy(kill.name, lionId)
        d.submitTargetSelection(p, listOf(bear)).error shouldBe null

        val projected = d.state.projectedState
        projected.hasKeyword(bear, Keyword.FLYING) shouldBe true
        projected.getPower(bear) shouldBe 3
        projected.getToughness(bear) shouldBe 3
        val life = d.getLifeTotal(p)
        d.submit(ActivateAbility(p, lionId, lionPump.id)).error shouldBe null
        repeat(4) { if (d.state.stack.isNotEmpty()) d.bothPass() }
        d.getLifeTotal(p) shouldBe life + 1
    }

    test("it returns under the dying creature's controller, whoever owns the card (ruling)") {
        val d = driver()
        val p = d.activePlayer!!
        val opp = d.getOpponent(p)
        val numenId = d.putCreatureOnBattlefield(opp, numen.name)
        d.addComponent(numenId, ControllerComponent(p))
        val myForest = d.putLandOnBattlefield(p, "Forest")
        d.putLandOnBattlefield(opp, "Forest")

        d.destroy(kill.name, numenId)
        val decision = d.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        decision.playerId shouldBe p
        decision.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(myForest)
        d.submitTargetSelection(p, listOf(myForest)).error shouldBe null

        d.state.projectedState.getController(numenId) shouldBe p
        d.state.getEntity(numenId)!!.get<AttachedToComponent>()!!.targetId shouldBe myForest
    }
})
