package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The card that moves itself out of a named zone, and the ability field CR 113.6m derives from it.
 *
 * Two things are under test and they are different: the [Recursion] product — the move table, its
 * placement rider and its counter rider — and the *derivation*, which is what makes an ability with a
 * non-battlefield activation zone printable at all.
 */
class RecursionTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun roundTrips(line: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe line
    }

    fun declines(line: String) {
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    // Sanitarium Skeleton's golden, field for field: the guard on the effect and the zone on the
    // ability, from one printed "from your graveyard".
    "the source zone lands on the effect and on the ability at once" {
        fragment("{2}{B}: Return ~ from your graveyard to your hand.") shouldBe CardFragment(
            script = CardScript(
                activatedAbilities = listOf(
                    ActivatedAbility(
                        id = AbilityId("activated"),
                        cost = AbilityCost.Atom(com.wingedsheep.sdk.scripting.costs.CostAtom.Mana(
                            com.wingedsheep.sdk.core.ManaCost.parse("{2}{B}")
                        )),
                        effect = Effects.ReturnToHandFromGraveyard(EffectTarget.Self),
                        activateFromZone = Zone.GRAVEYARD,
                    )
                )
            )
        )
        roundTrips("{2}{B}: Return ~ from your graveyard to your hand.")
    }

    // Reassembling Skeleton, Haunted Dead, Tunnel Rats: the placement rider, and the empty row of it.
    "the placement rider is a row, so the bare sentence and the tapped one both read" {
        roundTrips("{1}{B}: Return ~ from your graveyard to the battlefield.")
        roundTrips("{1}{B}: Return ~ from your graveyard to the battlefield tapped.")
        roundTrips("{2}{R}: Return ~ from your graveyard to the battlefield tapped and attacking.")
    }

    // Relentless X-ATM092 and Gastal Thrillroller — a composite of the move and an AddCounters on the
    // source, which is how the corpus spells "with a finality counter on it".
    "the counter rider composes onto the move" {
        fragment("{8}: Return ~ from your graveyard to the battlefield tapped with a finality counter on it.")
            .script.activatedAbilities.single().effect shouldBe Effects.Composite(
            listOf(
                Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.Self, tapped = true),
                Effects.AddCounters("finality", 1, EffectTarget.Self),
            )
        )
        roundTrips("{8}: Return ~ from your graveyard to the battlefield tapped with a finality counter on it.")
        roundTrips("{3}{B}: Return ~ from your graveyard to the battlefield tapped with two +1/+1 counters on it.")
    }

    // Urban Retreat: the zone is a column of the table, not a constant, and the verb agrees with the
    // pair — "put … onto" from the hand where the graveyard says "return … to".
    "the hand is a row of the same table" {
        val ability = fragment("{4}: Put ~ from your hand onto the battlefield.")
            .script.activatedAbilities.single()
        ability.activateFromZone shouldBe Zone.HAND
        ability.effect shouldBe Effects.Move(EffectTarget.Self, Zone.BATTLEFIELD, fromZone = Zone.HAND)
        roundTrips("{4}: Put ~ from your hand onto the battlefield.")
    }

    // Garland, Knight of Cornelia. "Transformed" is a different SDK type, not a ZonePlacement, and
    // the derivation reads its `fromZone` too.
    "the transformed return is its own effect and still derives its zone" {
        val ability = fragment("{3}{B}{B}{R}{R}: Return ~ from your graveyard to the battlefield transformed. Activate only as a sorcery.")
            .script.activatedAbilities.single()
        ability.activateFromZone shouldBe Zone.GRAVEYARD
        ability.effect shouldBe Effects.ReturnSelfFromGraveyardTransformed()
        roundTrips("{3}{B}{B}{R}{R}: Return ~ from your graveyard to the battlefield transformed. Activate only as a sorcery.")
    }

    // "Put ~ from your graveyard onto the battlefield" is the same model as the "return … to" row, so
    // it parses and prints as the canonical spelling rather than as itself.
    "the second verb is an alternate spelling, not a second model" {
        val put = fragment("{3}: Put ~ from your graveyard onto the battlefield tapped.")
        put shouldBe fragment("{3}: Return ~ from your graveyard to the battlefield tapped.")
        Grammar.abilityLine.printLine(put) shouldBe "{3}: Return ~ from your graveyard to the battlefield tapped."
    }

    // CR 113.6m's "unless … a previous part of its cost … specifies that the object is put into that
    // zone": a cost that sacrifices the source is what put it in the graveyard, so the ability still
    // functions on the battlefield.
    "a cost that puts the source into the zone cancels the derivation" {
        fragment("Sacrifice ~: Return ~ from your graveyard to your hand.")
            .script.activatedAbilities.single()
            .activateFromZone shouldBe Zone.BATTLEFIELD
    }

    // The other half of the same "unless": Bloodghast's landfall trigger works from the graveyard,
    // and a dies trigger returning the same card does not — the trigger condition is what put it
    // there. (The Ojer cycle; see [Recursion.functionsIn].)
    "the trigger condition decides whether a trigger functions in the zone" {
        val landfall = fragment("Whenever a land you control enters, you may return ~ from your graveyard to the battlefield.")
            .script.triggeredAbilities.single()
        landfall.activeZones shouldBe setOf(Zone.GRAVEYARD)

        val dies = TriggeredAbility(
            id = AbilityId("trigger"),
            trigger = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Any,
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD,
            ),
            binding = TriggerBinding.SELF,
            effect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.Self),
            activeZones = setOf(Zone.BATTLEFIELD),
        )
        // The model a dies-and-return card carries is printable, and printing it does not move the
        // ability into the graveyard.
        Recursion.functionsIn(
            effect = dies.effect,
            putsSourceInto = Zone.GRAVEYARD,
        ) shouldBe null
        Recursion.functionsIn(effect = dies.effect) shouldBe Zone.GRAVEYARD
    }

    // The restriction sentence the family so often ends on. "During your upkeep" is one printed
    // phrase and one `All` of two restrictions, not two rows of the comma-joined run.
    "the activation restrictions the family prints" {
        fragment("{3}{W}{W}: Return ~ from your graveyard to your hand. Activate only during your upkeep.")
            .script.activatedAbilities.single()
            .restrictions shouldBe listOf(
            ActivationRestriction.All(
                ActivationRestriction.OnlyDuringYourTurn,
                ActivationRestriction.DuringStep(Step.UPKEEP),
            )
        )
        roundTrips("{3}{W}{W}: Return ~ from your graveyard to your hand. Activate only during your upkeep.")
        roundTrips("{1}{B}: Return ~ from your graveyard to your hand. Activate only during your turn.")
    }

    // A battlefield ability is untouched by any of this: nothing the grammar built before carried a
    // `fromZone`, which is why the derivation cannot change an existing reading.
    "an ordinary battlefield ability keeps its default zone" {
        fragment("{T}: Draw a card.").script.activatedAbilities.single()
            .activateFromZone shouldBe Zone.BATTLEFIELD
        fragment("{2}: Put a +1/+1 counter on ~.").script.activatedAbilities.single()
            .activateFromZone shouldBe Zone.BATTLEFIELD
        fragment("{T}: Add {G}.").script.activatedAbilities.single()
            .activateFromZone shouldBe Zone.BATTLEFIELD
    }

    // The riders belong to the rows that print them: a hand return is never tapped and never lands
    // with counters, so offering it those would print sentences Oracle does not have.
    "a row declares which riders it takes" {
        declines("{2}{B}: Return ~ from your graveyard to your hand tapped.")
        declines("{2}{B}: Return ~ from your graveyard to your hand with a finality counter on it.")
        declines("{2}{U}: Put ~ from your graveyard on top of your library tapped.")
    }

    // The ZonePlacement the library row fixes is not the rider's business either.
    "the library row carries its own placement" {
        fragment("{5}{B}{B}: Put ~ from your graveyard on top of your library.")
            .script.activatedAbilities.single().effect shouldBe
            Effects.Move(EffectTarget.Self, Zone.LIBRARY, placement = ZonePlacement.Top, fromZone = Zone.GRAVEYARD)
        roundTrips("{5}{B}{B}: Put ~ from your graveyard on top of your library.")
    }

    // A guard is not decoration: the two models differ, so a golden that omits it diverges rather
    // than folding. That is what reported 18 cards.
    "the guarded move is a different model from the unguarded one" {
        Effects.ReturnToHandFromGraveyard(EffectTarget.Self) shouldNotBe
            Effects.ReturnToHand(EffectTarget.Self)
    }
})
