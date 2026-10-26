package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import com.wingedsheep.sdk.dsl.Triggers

/**
 * End-to-end test for the Long List of the Ents (LTR) chapter shape:
 *
 *   1. `NoteCreatureType("notedType")` — picks a type, persists it on the source's
 *      `NotedCreatureTypesComponent`, stores it in `chosenValues["notedType"]`.
 *   2. `CreateDelayedTriggerEffect(trigger = "you cast a creature spell with subtype
 *      `chosenValues["notedType"]` this turn", fireOnce, expiry = EndOfTurn,
 *      effect = AddCounters(+1/+1, 1, TriggeringEntity))` — installs a one-shot
 *      delayed triggered ability scoped to the noted type.
 *
 * The chosen value must be *baked into the trigger filter* at trigger-creation time
 * (`CreateDelayedTriggerExecutor.bakeChosenValuesIntoTrigger`) because the
 * EffectContext that holds chosenValues is gone by the time the trigger fires.
 *
 * Engine guarantees under test:
 *  1. Casting a creature spell of the noted type fires the trigger, and the entering
 *     creature ends up with a +1/+1 counter.
 *  2. Casting a creature spell of a *different* type does NOT fire the trigger.
 *  3. `fireOnce = true` and `expiry = EndOfTurn` make the trigger a one-shot —
 *     the second cast of the noted type does not double-stack the counter.
 *  4. Two activations on the same source choose two different types, and each gets
 *     its own one-shot trigger (so casting one of each adds a counter to each).
 */
class DelayedTriggerFromNotedTypeTest : FunSpec({

    val ChapterMimic = card("Chapter Mimic") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        oracleText = "{T}: Note a creature type that hasn't been noted for this enchantment. " +
            "When you next cast a creature spell of that type this turn, that creature " +
            "enters with an additional +1/+1 counter on it."
        activatedAbility {
            cost = Costs.Tap
            effect = CompositeEffect(listOf(
                Effects.NoteCreatureType("notedType"),
                CreateDelayedTriggerEffect(
                    trigger = Triggers.you.casts(GameObjectFilter.Creature.withSubtypeFromVariable("notedType")),
                    fireOnce = true,
                    expiry = DelayedTriggerExpiry.EndOfTurn,
                    effect = Effects.AddCounters(
                        com.wingedsheep.sdk.core.CounterType.PLUS_ONE_PLUS_ONE,
                        1,
                        EffectTarget.TriggeringEntity
                    )
                )
            ))
        }
    }

    val ElfWarrior = CardDefinition.creature(
        "Elf Warrior Test", ManaCost.parse("{1}{G}"),
        setOf(com.wingedsheep.sdk.core.Subtype("Elf"), com.wingedsheep.sdk.core.Subtype("Warrior")),
        2, 2
    )
    val GoblinScout = CardDefinition.creature(
        "Goblin Scout Test", ManaCost.parse("{R}"),
        setOf(com.wingedsheep.sdk.core.Subtype("Goblin"), com.wingedsheep.sdk.core.Subtype("Scout")),
        1, 1
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ChapterMimic, ElfWarrior, GoblinScout))
        return driver
    }

    val abilityId = ChapterMimic.activatedAbilities.first().id

    test("Noted type — casting a creature of that type enters with an extra +1/+1 counter") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val source = driver.putPermanentOnBattlefield(active, "Chapter Mimic")
        driver.submit(ActivateAbility(playerId = active, sourceId = source, abilityId = abilityId))
        driver.bothPass()

        val choice = driver.pendingDecision as ChooseOptionDecision
        driver.submitDecision(active, OptionChosenResponse(choice.id, choice.options.indexOf("Elf")))

        // Sanity: noted set updated.
        driver.state.getEntity(source)
            ?.get<com.wingedsheep.engine.state.components.battlefield.NotedCreatureTypesComponent>()
            ?.types shouldBe setOf("Elf")

        // Cast an Elf creature — the delayed trigger fires on cast and owes the spell a +1/+1
        // counter, given to the permanent as it enters (CR 122.6).
        val elfCardId = driver.putCardInHand(active, "Elf Warrior Test")
        driver.giveMana(active, Color.GREEN, 1)
        driver.giveColorlessMana(active, 1)
        driver.castSpell(active, elfCardId)
        while (driver.stackSize > 0) driver.bothPass()

        val elfPlus = driver.state.getEntity(elfCardId)
            ?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        elfPlus shouldBe 1
    }

    test("Different creature type does not fire the trigger — no counter added") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val source = driver.putPermanentOnBattlefield(active, "Chapter Mimic")
        driver.submit(ActivateAbility(playerId = active, sourceId = source, abilityId = abilityId))
        driver.bothPass()
        val choice = driver.pendingDecision as ChooseOptionDecision
        driver.submitDecision(active, OptionChosenResponse(choice.id, choice.options.indexOf("Elf")))

        // Cast a Goblin (not an Elf) — trigger must NOT fire.
        val goblinCardId = driver.putCardInHand(active, "Goblin Scout Test")
        driver.giveMana(active, Color.RED, 1)
        driver.castSpell(active, goblinCardId)
        while (driver.stackSize > 0) driver.bothPass()

        val goblinPlus = driver.state.getEntity(goblinCardId)
            ?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        goblinPlus shouldBe 0
    }

    test("fireOnce = true — the second cast of the noted type doesn't double-add a counter") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val source = driver.putPermanentOnBattlefield(active, "Chapter Mimic")
        driver.submit(ActivateAbility(playerId = active, sourceId = source, abilityId = abilityId))
        driver.bothPass()
        val choice = driver.pendingDecision as ChooseOptionDecision
        driver.submitDecision(active, OptionChosenResponse(choice.id, choice.options.indexOf("Elf")))

        // First Elf — gets the counter.
        val first = driver.putCardInHand(active, "Elf Warrior Test")
        driver.giveMana(active, Color.GREEN, 1)
        driver.giveColorlessMana(active, 1)
        driver.castSpell(active, first)
        while (driver.stackSize > 0) driver.bothPass()
        driver.state.getEntity(first)
            ?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1

        // Second Elf — trigger was one-shot, must NOT fire again.
        val second = driver.putCardInHand(active, "Elf Warrior Test")
        driver.giveMana(active, Color.GREEN, 1)
        driver.giveColorlessMana(active, 1)
        driver.castSpell(active, second)
        while (driver.stackSize > 0) driver.bothPass()
        val secondCounters = driver.state.getEntity(second)
            ?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        secondCounters shouldBe 0
    }

    test("Two activations on the same source — two independent one-shot triggers, one per noted type") {
        // The multi-chapter Long List of the Ents shape: each activation notes a different type and
        // installs its own delayed trigger. Casting one creature of each type adds a counter to each.
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val source = driver.putPermanentOnBattlefield(active, "Chapter Mimic")

        // First activation — note Elf.
        driver.submit(ActivateAbility(playerId = active, sourceId = source, abilityId = abilityId))
        driver.bothPass()
        var choice = driver.pendingDecision as ChooseOptionDecision
        driver.submitDecision(active, OptionChosenResponse(choice.id, choice.options.indexOf("Elf")))

        // Untap the source so it can be activated again this turn.
        driver.state.updateEntity(source) {
            it.without<com.wingedsheep.engine.state.components.battlefield.TappedComponent>()
        }.let { driver.replaceState(it) }

        // Second activation — note Goblin (Elf is excluded from the option list).
        driver.submit(ActivateAbility(playerId = active, sourceId = source, abilityId = abilityId))
        driver.bothPass()
        choice = driver.pendingDecision as ChooseOptionDecision
        driver.submitDecision(active, OptionChosenResponse(choice.id, choice.options.indexOf("Goblin")))

        // Cast one of each type — each spell trips its own type's one-shot trigger. Drain the whole
        // stack (the creature spell plus its delayed trigger) before the second sorcery-speed cast.
        val elf = driver.putCardInHand(active, "Elf Warrior Test")
        driver.giveMana(active, Color.GREEN, 1)
        driver.giveColorlessMana(active, 1)
        driver.castSpell(active, elf)
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        val goblin = driver.putCardInHand(active, "Goblin Scout Test")
        driver.giveMana(active, Color.RED, 1)
        driver.castSpell(active, goblin)
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.state.getEntity(elf)
            ?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        driver.state.getEntity(goblin)
            ?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
    }
})
