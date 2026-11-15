package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * [com.wingedsheep.sdk.scripting.effects.BecomeAuraEffect]: a permanent already on the battlefield
 * becomes an Aura with a new enchant ability and attaches itself to an object its ability made — the
 * Fate Reforged "Form" cycle ("When this enchantment enters, it becomes an Aura with enchant
 * creature. Manifest the top card of your library and attach this enchantment to it.").
 */
class BecomeAuraTest : FunSpec({
    fun form(name: String, enchant: GameObjectFilter) = card(name) {
        manaCost = "{U}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Patterns.Library.manifest() then
                Effects.BecomeAura(enchant, attachTo = EffectTarget.PipelineTarget("manifested"))
        }
        staticAbility {
            ability = GrantKeyword(Keyword.FLYING)
        }
    }
    val creatureForm = form("Test Creature Form", GameObjectFilter.Creature)
    val artifactForm = card("Test Artifact Form") {
        manaCost = "{U}"
        typeLine = "Artifact Enchantment"
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Patterns.Library.manifest() then
                Effects.BecomeAura(GameObjectFilter.Creature, attachTo = EffectTarget.PipelineTarget("manifested"))
        }
    }
    val blink = card("Test Form Blink") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val permanent = target(TargetFilter.Permanent)
            effect = Effects.Exile(permanent) then Effects.Move(permanent, Zone.BATTLEFIELD)
        }
    }
    val landForm = form("Test Land Form", GameObjectFilter.Land)
    val kill = card("Test Form Kill") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.Destroy(creature)
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(creatureForm, artifactForm, landForm, kill, blink))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.settle() {
        repeat(8) { if (state.pendingDecision == null && state.stack.isNotEmpty()) bothPass() }
    }

    /** Cast [name] and resolve the spell, leaving its enters trigger on the stack. */
    fun GameTestDriver.castForm(name: String): EntityId {
        val p = activePlayer!!
        giveMana(p, Color.BLUE, 1)
        val formId = putCardInHand(p, name)
        castSpell(p, formId).error shouldBe null
        bothPass()
        state.stack.size shouldBe 1
        return formId
    }

    fun GameTestDriver.manifestedBy(p: EntityId): EntityId? = getPermanents(p).firstOrNull {
        state.getEntity(it)?.get<FaceDownModeComponent>()?.mode == FaceDownMode.MANIFEST
    }

    test("before the trigger resolves it is a plain enchantment; then an Aura attached to the manifested creature") {
        val d = driver()
        val p = d.activePlayer!!
        val formId = d.castForm(creatureForm.name)
        d.state.projectedState.hasSubtype(formId, "Aura") shouldBe false

        d.settle()

        val host = d.manifestedBy(p)!!
        val projected = d.state.projectedState
        projected.hasType(formId, "ENCHANTMENT") shouldBe true
        projected.hasSubtype(formId, "Aura") shouldBe true
        d.state.getEntity(formId)?.get<AttachedToComponent>()?.targetId shouldBe host
        // It kept its own "enchanted creature has flying".
        projected.hasKeyword(host, Keyword.FLYING) shouldBe true
        (formId in d.state.getBattlefield()) shouldBe true
    }

    test("it stays an Aura: when the enchanted creature dies the Aura is put into the graveyard") {
        val d = driver()
        val p = d.activePlayer!!
        val formId = d.castForm(creatureForm.name)
        d.settle()
        val host = d.manifestedBy(p)!!

        d.castSpellWithTargets(p, d.putCardInHand(p, kill.name), listOf(ChosenTarget.Permanent(host))).error shouldBe null
        d.settle()

        (formId in d.state.getZone(ZoneKey(p, Zone.GRAVEYARD))) shouldBe true
    }

    test("with an empty library nothing is manifested and the unattached Aura goes to the graveyard") {
        val d = driver()
        val p = d.activePlayer!!
        val formId = d.castForm(creatureForm.name)
        val library = ZoneKey(p, Zone.LIBRARY)
        d.replaceState(d.state.getZone(library).fold(d.state) { s, id -> s.removeFromZone(library, id) })

        d.settle()

        d.manifestedBy(p).shouldBeNull()
        (formId in d.state.getZone(ZoneKey(p, Zone.GRAVEYARD))) shouldBe true
    }

    test("the manifest still happens when the source left the battlefield before its trigger resolved") {
        val d = driver()
        val p = d.activePlayer!!
        val formId = d.castForm(creatureForm.name)
        d.moveToGraveyard(formId)

        d.settle()

        val host = d.manifestedBy(p)!!
        (formId in d.state.getZone(ZoneKey(p, Zone.GRAVEYARD))) shouldBe true
        d.state.projectedState.hasKeyword(host, Keyword.FLYING) shouldBe false
    }

    test("an object its new enchant ability doesn't admit isn't attached to, and the Aura is put into the graveyard") {
        val d = driver()
        val p = d.activePlayer!!
        val formId = d.castForm(landForm.name)

        d.settle()

        d.state.pendingDecision?.let { (it is ChooseTargetsDecision) shouldBe false }
        val host = d.manifestedBy(p)!!
        d.state.getEntity(host)?.get<AttachedToComponent>().shouldBeNull()
        (formId in d.state.getZone(ZoneKey(p, Zone.GRAVEYARD))) shouldBe true
    }

    test("becoming an Aura keeps its other card types: an artifact enchantment stays an artifact") {
        val d = driver()
        val p = d.activePlayer!!
        val formId = d.castForm(artifactForm.name)

        d.settle()

        val projected = d.state.projectedState
        projected.hasType(formId, "ARTIFACT") shouldBe true
        projected.hasType(formId, "ENCHANTMENT") shouldBe true
        projected.hasSubtype(formId, "Aura") shouldBe true
        d.state.getEntity(formId)?.get<AttachedToComponent>()?.targetId shouldBe d.manifestedBy(p)
    }

    test("a source that left and returned is a new object: its old trigger manifests but doesn't move it") {
        val d = driver()
        val p = d.activePlayer!!
        val olderCard = d.putCardOnTopOfLibrary(p, "Centaur Courser")
        val formId = d.castForm(creatureForm.name)
        // Blink the Form in response to its trigger; the returned Form's own trigger resolves first.
        d.castSpellWithTargets(p, d.putCardInHand(p, blink.name), listOf(ChosenTarget.Permanent(formId))).error shouldBe null
        d.bothPass()
        val newerCard = d.putCardOnTopOfLibrary(p, "Savannah Lions")

        d.settle()

        val form = d.getPermanents(p).single { d.getCardName(it) == creatureForm.name }
        val host = d.state.getEntity(form)?.get<AttachedToComponent>()?.targetId
        host shouldBe newerCard
        d.state.getEntity(olderCard)?.get<FaceDownModeComponent>()?.mode shouldBe FaceDownMode.MANIFEST
    }
})
