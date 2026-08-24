package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.CreatureTypeRevealedEvent
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.NotedCreatureTypesComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.view.ClientCardEffect
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * The secret half of `NoteCreatureTypeEffect` and the `CostAtom.RevealNotedCreatureType` that
 * publishes it — the hidden-agenda pair (CR 702.106a-d) applied to a permanent, built for
 * A Killer Among Us and tested here on a bare card so the primitive is pinned independently of it.
 *
 * What the vocabulary promises, and therefore what must hold:
 *  1. `options` narrows the offered creature types to exactly that list, and the source's
 *     already-noted types still drop out of it.
 *  2. A secret note records *who* made it; an ordinary note records no one.
 *  3. The client view shows a secret note only to that player — never to an opponent, never to a
 *     spectator. A public note is shown to everyone.
 *  4. Paying the reveal cost publishes the note and emits a `CreatureTypeRevealedEvent`.
 *  5. Only the chooser can pay it: after a change of control the ability is not offered at all,
 *     and it comes back if control returns.
 */
class SecretCreatureTypeChoiceTest : FunSpec({

    /** "{T}: Secretly choose Alpha, Beta, or Gamma." — the write half, on its own. */
    val SecretChooser = card("Secret Chooser") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        oracleText = "{T}: Secretly choose Elf, Goblin, or Zombie."
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.SecretlyChooseCreatureType(listOf("Elf", "Goblin", "Zombie"))
        }
    }

    /** The public sibling, identical but for the secrecy — the control for tests 2 and 3. */
    val OpenChooser = card("Open Chooser") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        oracleText = "{T}: Note Elf, Goblin, or Zombie."
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.NoteCreatureType(options = listOf("Elf", "Goblin", "Zombie"))
        }
    }

    /** "Reveal the creature type you chose: Draw a card." — the read half, with nothing else in it. */
    val Revealer = card("Revealer") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        oracleText = "{T}: Secretly choose Elf, Goblin, or Zombie.\n" +
            "Reveal the creature type you chose: Draw a card."
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.SecretlyChooseCreatureType(listOf("Elf", "Goblin", "Zombie"))
        }
        activatedAbility {
            cost = Costs.RevealNotedCreatureType
            effect = Effects.DrawCards(1)
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SecretChooser, OpenChooser, Revealer))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Activate [abilityIndex] on [source] and answer the resulting option decision with [type]. */
    fun GameTestDriver.chooseOn(source: EntityId, cardName: String, type: String, abilityIndex: Int = 0) {
        val def = listOf(SecretChooser, OpenChooser, Revealer).single { it.name == cardName }
        submit(ActivateAbility(activePlayer!!, source, def.activatedAbilities[abilityIndex].id))
        bothPass()
        val decision = pendingDecision as ChooseOptionDecision
        submitDecision(activePlayer!!, OptionChosenResponse(decision.id, decision.options.indexOf(type)))
    }

    fun GameTestDriver.notedBadges(entityId: EntityId, viewer: EntityId, spectator: Boolean = false):
        List<ClientCardEffect> =
        ClientStateTransformer(cardRegistry).transform(state, viewer, spectator)
            .cards[entityId]
            ?.activeEffects
            ?.filter { it.effectId.startsWith("noted_creature_types") }
            .orEmpty()

    /** Untap [entityId] so a `{T}` ability can be activated again in the same turn. */
    fun GameTestDriver.untap(entityId: EntityId) {
        replaceState(state.updateEntity(entityId) { it.without<TappedComponent>() })
    }

    test("options narrows the offered types, and already-noted ones still drop out") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val source = driver.putPermanentOnBattlefield(active, "Secret Chooser")

        driver.submit(ActivateAbility(active, source, SecretChooser.activatedAbilities.first().id))
        driver.bothPass()
        val first = driver.pendingDecision as ChooseOptionDecision
        first.options.sorted() shouldContainExactly listOf("Elf", "Goblin", "Zombie")
        driver.submitDecision(active, OptionChosenResponse(first.id, first.options.indexOf("Goblin")))

        // Untap and go again: the type just noted is no longer on offer.
        driver.untap(source)
        driver.submit(ActivateAbility(active, source, SecretChooser.activatedAbilities.first().id))
        driver.bothPass()
        val second = driver.pendingDecision as ChooseOptionDecision
        second.options.sorted() shouldContainExactly listOf("Elf", "Zombie")
    }

    test("a secret note records its chooser; a public one records no one") {
        val driver = newDriver()
        val active = driver.activePlayer!!

        val secret = driver.putPermanentOnBattlefield(active, "Secret Chooser")
        driver.chooseOn(secret, "Secret Chooser", "Elf")
        driver.state.getEntity(secret)?.get<NotedCreatureTypesComponent>()?.secretTo shouldBe active

        val open = driver.putPermanentOnBattlefield(active, "Open Chooser")
        driver.chooseOn(open, "Open Chooser", "Elf")
        driver.state.getEntity(open)?.get<NotedCreatureTypesComponent>()?.secretTo.shouldBeNull()
    }

    test("the client view shows a secret note only to its chooser — not opponents, not spectators") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val opponent = driver.state.turnOrder.single { it != active }

        val source = driver.putPermanentOnBattlefield(active, "Secret Chooser")
        driver.chooseOn(source, "Secret Chooser", "Zombie")

        driver.notedBadges(source, active).single().description?.contains("Zombie") shouldBe true
        driver.notedBadges(source, opponent) shouldContainExactly emptyList()
        driver.notedBadges(source, active, spectator = true) shouldContainExactly emptyList()
    }

    test("a public note is shown to everyone") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val opponent = driver.state.turnOrder.single { it != active }

        val source = driver.putPermanentOnBattlefield(active, "Open Chooser")
        driver.chooseOn(source, "Open Chooser", "Zombie")

        driver.notedBadges(source, active).single().description?.contains("Zombie") shouldBe true
        driver.notedBadges(source, opponent).single().description?.contains("Zombie") shouldBe true
    }

    test("paying the reveal cost publishes the note and announces the type") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val opponent = driver.state.turnOrder.single { it != active }

        val source = driver.putPermanentOnBattlefield(active, "Revealer")
        driver.chooseOn(source, "Revealer", "Elf")
        driver.untap(source)

        val handBefore = driver.getHandSize(active)
        driver.submit(ActivateAbility(active, source, Revealer.activatedAbilities[1].id))
        driver.bothPass()

        driver.getHandSize(active) shouldBe handBefore + 1
        driver.state.getEntity(source)?.get<NotedCreatureTypesComponent>()?.secretTo.shouldBeNull()
        driver.events.filterIsInstance<CreatureTypeRevealedEvent>()
            .map { it.revealedType } shouldContainExactly listOf("Elf")
        // And now the opponent can see it.
        driver.notedBadges(source, opponent).single().description?.contains("Elf") shouldBe true
    }

    test("the ability is offered only while the activating player is the one who chose") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val opponent = driver.state.turnOrder.single { it != active }
        val revealAbility = Revealer.activatedAbilities[1].id

        val source = driver.putPermanentOnBattlefield(active, "Revealer")

        // Nothing noted yet — there is no choice to reveal, so the ability isn't offered.
        driver.legalActions(active)
            .none { (it.action as? ActivateAbility)?.abilityId == revealAbility } shouldBe true

        driver.chooseOn(source, "Revealer", "Goblin")
        driver.untap(source)
        driver.legalActions(active)
            .any { (it.action as? ActivateAbility)?.abilityId == revealAbility } shouldBe true

        // Hand it over: the new controller never saw the choice and can't reveal it. Not greyed
        // out — absent, because no board state could ever make it payable for them.
        driver.replaceState(
            driver.state.updateEntity(source) { c ->
                c.with(com.wingedsheep.engine.state.components.identity.ControllerComponent(opponent))
            }
        )
        driver.legalActions(opponent)
            .none { (it.action as? ActivateAbility)?.abilityId == revealAbility } shouldBe true

        // Give it back and it returns — the note never moved, only the ability's availability.
        driver.replaceState(
            driver.state.updateEntity(source) { c ->
                c.with(com.wingedsheep.engine.state.components.identity.ControllerComponent(active))
            }
        )
        driver.legalActions(active)
            .any { (it.action as? ActivateAbility)?.abilityId == revealAbility } shouldBe true
    }
})
