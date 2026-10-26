package com.wingedsheep.engine.tracking

import com.wingedsheep.engine.state.components.battlefield.DamageSourceLki
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Engine coverage for the frozen damage-source record behind
 * `StatePredicate.WasDealtDamageThisTurnBy`: a resolving spell's card types are on the record, so
 * "a spell you controlled" (`InstantOrSorcery.youControl()`) can be asked of it, alone or in an
 * `or` union with subtypes ("a Giant, Wizard, or spell" — Aegar, the Freezing Flame); predicates
 * the record can't answer still fail closed.
 */
class DamageSourceRecordMatchingTest : FunSpec({

    val you = EntityId("you")
    val opponent = EntityId("opponent")

    val bolt = DamageSourceLki(
        sourceControllerId = you,
        sourceWasCreature = false,
        sourceCardTypes = setOf(CardType.INSTANT),
        sourceName = "Lightning Bolt",
    )
    val giant = DamageSourceLki(
        sourceControllerId = you,
        sourceSubtypes = setOf(Subtype("Giant")),
        sourceCardTypes = setOf(CardType.CREATURE),
    )
    val bear = DamageSourceLki(
        sourceControllerId = you,
        sourceSubtypes = setOf(Subtype("Bear")),
        sourceCardTypes = setOf(CardType.CREATURE),
    )

    val giantWizardOrSpell = (
        GameObjectFilter.Any.withSubtype("Giant") or
            GameObjectFilter.Any.withSubtype("Wizard") or
            GameObjectFilter.InstantOrSorcery
        ).youControl()

    test("an instant or sorcery source matches a spell filter, read relative to you") {
        bolt.matches(GameObjectFilter.InstantOrSorcery.youControl(), you) shouldBe true
        bolt.matches(GameObjectFilter.InstantOrSorcery.youControl(), opponent) shouldBe false
        bolt.matches(GameObjectFilter.Creature, you) shouldBe false
    }

    test("a subtype-or-spell union matches each branch and nothing else") {
        bolt.matches(giantWizardOrSpell, you) shouldBe true
        giant.matches(giantWizardOrSpell, you) shouldBe true
        bear.matches(giantWizardOrSpell, you) shouldBe false
    }

    test("a multi-predicate branch of an or union is matched whole") {
        val giantCreatureOrSpell = GameObjectFilter.Creature.withSubtype("Giant") or GameObjectFilter.InstantOrSorcery
        giant.matches(giantCreatureOrSpell, you) shouldBe true
        bear.matches(giantCreatureOrSpell, you) shouldBe false
        bolt.matches(giantCreatureOrSpell, you) shouldBe true
    }

    test("a record written before card types were kept never matches a spell filter") {
        DamageSourceLki(sourceControllerId = you, sourceWasCreature = false)
            .matches(GameObjectFilter.InstantOrSorcery, you) shouldBe false
    }

    test("predicates the record can't answer fail closed") {
        bolt.matches(GameObjectFilter.Any.tapped(), you) shouldBe false
    }
})
