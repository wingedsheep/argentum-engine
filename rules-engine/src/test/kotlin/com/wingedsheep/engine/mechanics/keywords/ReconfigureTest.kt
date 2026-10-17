package com.wingedsheep.engine.mechanics.keywords

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Reconfigure (CR 702.151) and the attachment rules it changes:
 *  - 702.151a: two sorcery-speed activated abilities — attach to *another* target creature you
 *    control, and unattach, the latter only while attached to a creature;
 *  - 702.151b: attaching an Equipment with reconfigure to a creature — by any means — makes it stop
 *    being a creature (and lose its creature subtypes) until it becomes unattached from that creature,
 *    an effect of the attaching that survives the Equipment losing its abilities (ruling);
 *  - 301.5c: an Equipment that's also a creature can equip only if it has reconfigure;
 *  - rulings: reconfigure is not an equip ability.
 */
class ReconfigureTest : FunSpec({

    val blades = card("Reconfigure Test Blades") {
        manaCost = "{1}{R}"
        typeLine = "Artifact Creature — Equipment Lizard"
        power = 1
        toughness = 1
        keywords(Keyword.DOUBLE_STRIKE)
        staticAbility { ability = GrantKeyword(Keyword.DOUBLE_STRIKE, Filters.EquippedCreature) }
        reconfigure("{2}")
    }

    // Brass Squire's shape: attaches an Equipment by an effect other than its reconfigure ability.
    val squire = card("Reconfigure Test Squire") {
        manaCost = "{3}"
        typeLine = "Artifact Creature — Myr"
        power = 1
        toughness = 3
        activatedAbility {
            cost = Costs.Free
            val equipment = target(TargetFilter(GameObjectFilter.Artifact.withSubtype("Equipment").youControl()))
            val creature = target(TargetFilter.CreatureYouControl)
            effect = Effects.AttachTargetEquipmentToCreature(equipment, creature)
        }
    }

    // Strips every artifact's abilities — reconfigure included.
    val nullifier = card("Reconfigure Test Nullifier") {
        manaCost = "{2}"
        typeLine = "Enchantment"
        staticAbility { ability = LoseAllAbilities(GroupFilter(GameObjectFilter.Artifact)) }
    }

    val executioner = card("Reconfigure Test Executioner") {
        manaCost = "{2}"
        typeLine = "Enchantment"
        activatedAbility {
            cost = Costs.Free
            val victim = target(TargetFilter.Creature)
            effect = Effects.Destroy(victim)
        }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + blades + squire + nullifier + executioner)
        initMirrorMatch(deck = Deck.of("Mountain" to 30))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    val attachAbility = blades.activatedAbilities[0]
    val unattachAbility = blades.activatedAbilities[1]

    fun GameTestDriver.reconfigureOnto(source: EntityId, host: EntityId) {
        giveColorlessMana(player1, 2)
        submit(ActivateAbility(player1, source, attachAbility.id, listOf(ChosenTarget.Permanent(host))))
            .outcome shouldBe Outcome.Done
        bothPass()
    }

    fun GameTestDriver.attachedTo(id: EntityId): EntityId? = state.getEntity(id)?.get<AttachedToComponent>()?.targetId

    fun GameTestDriver.offered(source: EntityId, ability: ActivatedAbility): Boolean =
        legalActions(player1).any { (it.action as? ActivateAbility)?.let { a -> a.sourceId == source && a.abilityId == ability.id } == true }

    test("lowers to two sorcery-speed abilities that aren't equip abilities, plus the keyword") {
        blades.keywords.contains(Keyword.RECONFIGURE) shouldBe true
        blades.activatedAbilities.size shouldBe 2
        blades.activatedAbilities.all { it.timing == com.wingedsheep.sdk.scripting.TimingRule.SorcerySpeed } shouldBe true
        blades.activatedAbilities.any { it.isEquipAbility } shouldBe false
    }

    test("attaching makes it stop being a creature and lose its creature subtypes; the host gets the bonus") {
        val d = driver()
        val sash = d.putPermanentOnBattlefield(d.player1, blades.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")

        d.reconfigureOnto(sash, bears)

        d.attachedTo(sash) shouldBe bears
        val projected = d.state.projectedState
        projected.isCreature(sash) shouldBe false
        projected.hasType(sash, "ARTIFACT") shouldBe true
        projected.hasSubtype(sash, "Equipment") shouldBe true
        withClue("creature subtypes go with the creature type (CR 205.1a)") {
            projected.hasSubtype(sash, "Lizard") shouldBe false
        }
        projected.hasKeyword(bears, Keyword.DOUBLE_STRIKE) shouldBe true
    }

    test("the attach ability targets only another creature you control") {
        val d = driver()
        val sash = d.putPermanentOnBattlefield(d.player1, blades.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val theirs = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.giveColorlessMana(d.player1, 2)

        val action = d.legalActions(d.player1).first {
            (it.action as? ActivateAbility)?.abilityId == attachAbility.id
        }
        withClue("not itself, not an opponent's creature") { action.validTargets?.toSet() shouldBe setOf(bears) }
        d.submit(ActivateAbility(d.player1, sash, attachAbility.id, listOf(ChosenTarget.Permanent(sash))))
            .error shouldNotBe null
        d.submit(ActivateAbility(d.player1, sash, attachAbility.id, listOf(ChosenTarget.Permanent(theirs))))
            .error shouldNotBe null
    }

    test("unattach is offered only while attached to a creature, and makes it a creature again") {
        val d = driver()
        val sash = d.putPermanentOnBattlefield(d.player1, blades.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.giveColorlessMana(d.player1, 2)
        withClue("unattached: no unattach ability") { d.offered(sash, unattachAbility) shouldBe false }

        d.reconfigureOnto(sash, bears)
        d.giveColorlessMana(d.player1, 4)
        d.offered(sash, unattachAbility) shouldBe true
        withClue("unattaching is idempotent, so no 'activate how many times?' prompt") {
            d.legalActions(d.player1).first { (it.action as? ActivateAbility)?.abilityId == unattachAbility.id }
                .maxRepeatableActivations shouldBe null
        }

        d.submit(ActivateAbility(d.player1, sash, unattachAbility.id)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.attachedTo(sash) shouldBe null
        d.state.projectedState.isCreature(sash) shouldBe true
        d.state.projectedState.hasSubtype(sash, "Lizard") shouldBe true
        d.state.projectedState.hasKeyword(bears, Keyword.DOUBLE_STRIKE) shouldBe false
    }

    test("the attach ability is sorcery-speed: not offered outside a main phase") {
        val d = driver()
        val sash = d.putPermanentOnBattlefield(d.player1, blades.name)
        d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.passPriorityUntil(Step.BEGIN_COMBAT)
        d.giveColorlessMana(d.player1, 2)
        d.offered(sash, attachAbility) shouldBe false
    }

    test("moving onto another creature keeps it a non-creature") {
        val d = driver()
        val sash = d.putPermanentOnBattlefield(d.player1, blades.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val other = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")

        d.reconfigureOnto(sash, bears)
        d.reconfigureOnto(sash, other)

        d.attachedTo(sash) shouldBe other
        d.state.projectedState.isCreature(sash) shouldBe false
        d.state.projectedState.hasKeyword(other, Keyword.DOUBLE_STRIKE) shouldBe true
        d.state.projectedState.hasKeyword(bears, Keyword.DOUBLE_STRIKE) shouldBe false
        withClue("the effect from the first host is latched off; only the new host's remains") {
            d.state.floatingEffects.filter { it.duration == Duration.WhileAffectedAttachedToSource }
                .map { it.sourceId } shouldBe listOf(other)
        }
    }

    test("unattached and re-attached to the same creature: a fresh effect, and still not a creature") {
        val d = driver()
        val sash = d.putPermanentOnBattlefield(d.player1, blades.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")

        d.reconfigureOnto(sash, bears)
        d.giveColorlessMana(d.player1, 2)
        d.submit(ActivateAbility(d.player1, sash, unattachAbility.id)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.state.projectedState.isCreature(sash) shouldBe true
        d.state.floatingEffects.count { it.duration == Duration.WhileAffectedAttachedToSource } shouldBe 0

        d.reconfigureOnto(sash, bears)
        d.state.projectedState.isCreature(sash) shouldBe false
        d.state.floatingEffects.count { it.duration == Duration.WhileAffectedAttachedToSource } shouldBe 1
    }

    test("when the equipped creature dies, the Equipment is unattached and is a creature again") {
        val d = driver()
        val sash = d.putPermanentOnBattlefield(d.player1, blades.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val axe = d.putPermanentOnBattlefield(d.player1, executioner.name)
        d.reconfigureOnto(sash, bears)

        d.submit(
            ActivateAbility(d.player1, axe, executioner.activatedAbilities.single().id, listOf(ChosenTarget.Permanent(bears)))
        ).outcome shouldBe Outcome.Done
        d.bothPass()

        d.attachedTo(sash) shouldBe null
        d.state.projectedState.isCreature(sash) shouldBe true
    }

    test("attaching it by another effect also makes it stop being a creature (ruling)") {
        val d = driver()
        val sash = d.putPermanentOnBattlefield(d.player1, blades.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val myr = d.putPermanentOnBattlefield(d.player1, squire.name)

        d.submit(
            ActivateAbility(
                d.player1, myr, squire.activatedAbilities.single().id,
                listOf(ChosenTarget.Permanent(sash), ChosenTarget.Permanent(bears))
            )
        ).outcome shouldBe Outcome.Done
        d.bothPass()

        d.attachedTo(sash) shouldBe bears
        d.state.projectedState.isCreature(sash) shouldBe false
    }

    test("losing its abilities while attached doesn't make it a creature again (ruling)") {
        val d = driver()
        val sash = d.putPermanentOnBattlefield(d.player1, blades.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.reconfigureOnto(sash, bears)

        d.putPermanentOnBattlefield(d.player1, nullifier.name)
        d.giveMana(d.player1, Color.RED, 1) // any action settles state-based actions
        d.bothPass()

        d.state.projectedState.hasKeyword(sash, Keyword.RECONFIGURE) shouldBe false
        d.state.projectedState.isCreature(sash) shouldBe false
        d.attachedTo(sash) shouldBe bears
    }

    test("without reconfigure, an Equipment creature can't be attached (CR 301.5c)") {
        val d = driver()
        val sash = d.putPermanentOnBattlefield(d.player1, blades.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val myr = d.putPermanentOnBattlefield(d.player1, squire.name)
        d.putPermanentOnBattlefield(d.player1, nullifier.name)

        d.submit(
            ActivateAbility(
                d.player1, myr, squire.activatedAbilities.single().id,
                listOf(ChosenTarget.Permanent(sash), ChosenTarget.Permanent(bears))
            )
        )
        d.bothPass()

        d.attachedTo(sash) shouldBe null
        d.state.projectedState.isCreature(sash) shouldBe true
    }
})
