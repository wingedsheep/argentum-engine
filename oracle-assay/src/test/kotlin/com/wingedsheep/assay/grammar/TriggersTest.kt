package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.dsl.Triggers as SdkTriggers
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The trigger prefix: the first rules that reach a `CardScript` slot other than the spell effect,
 * and the first that depend on normalization abstracting a card's self-reference.
 */
class TriggersTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun roundTrips(line: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe line
    }

    fun ability(line: String): TriggeredAbility =
        fragment(line).script.triggeredAbilities.single()

    // Kavu Climber's golden is this model exactly, down to the trigger's serialized shape — which
    // is the point of parsing into `mtg-sdk` types rather than into an IR of our own.
    "an ETB trigger is the ability a card author writes from the same sentence" {
        ability("When ~ enters, draw a card.") shouldBe TriggeredAbility(
            id = AbilityId("trigger"),
            trigger = SdkTriggers.EntersBattlefield.event,
            binding = SdkTriggers.EntersBattlefield.binding,
            effect = Effects.DrawCards(1),
        )
        roundTrips("When ~ enters, draw a card.")
    }

    "the effect clause is the whole step vocabulary, not a second grammar" {
        listOf(
            "When ~ enters, draw two cards.",
            "When ~ enters, destroy target creature.",
            "When ~ enters, exile target artifact or enchantment.",
            "When ~ dies, draw a card.",
            "Whenever ~ attacks, draw a card.",
            "Whenever ~ blocks, tap target creature an opponent controls.",
            "Whenever ~ deals combat damage to a player, draw a card.",
        ).forEach { roundTrips(it) }
    }

    // A `TriggeredAbility` keeps its target on the ability rather than on the script, so the lift
    // out of `Steps` has to move it there — and the differential compares the result against cards
    // that write `target("target", …)` inside `triggeredAbility { }`.
    "a targeted trigger declares its requirement on the ability" {
        val triggered = ability("When ~ enters, destroy target creature.")

        triggered.targetRequirement shouldBe Targets.permanent(GameObjectFilter.Creature)
        triggered.effect shouldBe Effects.Destroy(Targets.bound())
        fragment("When ~ enters, destroy target creature.").script.targetRequirements shouldBe emptyList()
    }

    "several trigger lines are several abilities, in printed order" {
        val first = fragment("When ~ enters, draw a card.")
        val second = fragment("Whenever ~ attacks, draw two cards.")
        val whole = first.merge(second)

        whole?.script?.triggeredAbilities?.map { it.trigger } shouldBe listOf(
            SdkTriggers.EntersBattlefield.event,
            SdkTriggers.Attacks.event,
        )
    }

    // The prefix itself is one slot over `Triggers.phase(step, player, binding)` and lives in
    // [Phases], whose own test covers every spelling it can take. What belongs *here* is the lift: a
    // step trigger's effect clause is the same English a spell prints, so it inherits the whole step
    // vocabulary exactly as an event trigger's does.
    "the step triggers are the same shape with a different prefix" {
        listOf(
            "At the beginning of your upkeep, draw a card.",
            "At the beginning of your end step, you gain 2 life.",
            "At the beginning of combat on your turn, target creature gets +1/+1 until end of turn.",
            "At the beginning of your first main phase, scry 1.",
            "At the beginning of each upkeep, ~ deals 1 damage to any target.",
            "At the beginning of each opponent's upkeep, you gain 1 life.",
        ).forEach { roundTrips(it) }

        ability("At the beginning of your upkeep, draw a card.").trigger shouldBe
            SdkTriggers.YourUpkeep.event
    }

    // "You may …" is one sentence with one model. A triggered ability used to spell it with an
    // `optional` flag where a spell used a `MayEffect`, and `Triggers.abilityFor` had to lower
    // between the two; the flag is gone from the SDK and a trigger's consent is the same gate a
    // spell's is, so this now asserts that nothing special happens at all.
    "a trigger's \"you may\" is the same consent gate a spell's is" {
        val optional = TriggeredAbility(
            id = AbilityId("trigger"),
            trigger = SdkTriggers.EntersBattlefield.event,
            binding = SdkTriggers.EntersBattlefield.binding,
            effect = MayEffect(Effects.DrawCards(1)),
        )

        Grammar.abilityLine.printLine(
            CardFragment(script = CardScript(triggeredAbilities = listOf(optional)))
        ) shouldBe "When ~ enters, you may draw a card."
        fragment("When ~ enters, you may draw a card.") shouldBe
            CardFragment(script = CardScript(triggeredAbilities = listOf(optional)))
    }

    // An intervening-if (CR 603.4) *is* spellable: it is the clause between the event and the
    // effect, and `interveningIf` is the SDK's slot for it. `Triggers.abilityFor` lifts the
    // clause's own gate into that slot rather than leaving a copy in the effect, which is what the
    // hand-written cards do and what keeps one printed form for one model.
    "an intervening-if is the trigger's own condition, not a second gate in the effect" {
        val conditioned = TriggeredAbility(
            id = AbilityId("trigger"),
            trigger = SdkTriggers.EntersBattlefield.event,
            binding = SdkTriggers.EntersBattlefield.binding,
            effect = Effects.DrawCards(1),
            interveningIf = Conditions.OpponentControlsMoreLands,
        )

        fragment("When ~ enters, if an opponent controls more lands than you, draw a card.") shouldBe
            CardFragment(script = CardScript(triggeredAbilities = listOf(conditioned)))
        Grammar.abilityLine.printLine(
            CardFragment(script = CardScript(triggeredAbilities = listOf(conditioned)))
        ) shouldBe "When ~ enters, if an opponent controls more lands than you, draw a card."
    }

    // The other half of the split (CR 603.2 vs CR 603.4). A `triggerRestriction` is a different
    // printed shape — "Whenever this creature attacks *while* you control a Dinosaur" — that the
    // engine reads only when the trigger fires. No trigger rule spells it, so an ability carrying
    // one must decline rather than print the "if" sentence, whose model differs.
    "a trigger restriction is not printable as an intervening-if" {
        val restricted = TriggeredAbility(
            id = AbilityId("trigger"),
            trigger = SdkTriggers.EntersBattlefield.event,
            binding = SdkTriggers.EntersBattlefield.binding,
            effect = Effects.DrawCards(1),
            triggerRestriction = Conditions.OpponentControlsMoreLands,
        )

        Grammar.abilityLine.printLine(
            CardFragment(script = CardScript(triggeredAbilities = listOf(restricted)))
        ) shouldBe null
    }

    // Fail-closed, the same rule the step matchers follow: an ability carrying anything the sentence
    // does not spell must refuse to print rather than print a sentence that drops it.
    //
    // The example used to be `oncePerTurn`, and the batch band's rider now spells that — which is
    // the point rather than a loosening: the property is about fields *no rule spells*, so the
    // witness moves to the next one. `triggersOnce` is the permanent-lifetime cap ("This ability
    // triggers only once."), a different printed sentence and still nobody's row.
    "an ability with content the prefix does not spell refuses to print" {
        val capped = TriggeredAbility(
            id = AbilityId("trigger"),
            trigger = SdkTriggers.EntersBattlefield.event,
            binding = SdkTriggers.EntersBattlefield.binding,
            effect = Effects.DrawCards(1),
            triggersOnce = true,
        )

        Grammar.abilityLine.printLine(
            CardFragment(script = CardScript(triggeredAbilities = listOf(capped)))
        ) shouldBe null
    }

    // Valiant. The whole configuration is one published `TriggerSpec`, so the rule calls the
    // lowering rather than restating `BecomesTargetEvent`'s flags — and the once-each-turn cap is
    // part of the event here rather than the ability's `oncePerTurn`, which the fail-closed match
    // still refuses to print.
    "the valiant trigger is the spec the SDK publishes" {
        fragment(
            "Whenever ~ becomes the target of a spell or ability you control for the first time " +
                "each turn, draw a card."
        ).script.triggeredAbilities.single().trigger shouldBe SdkTriggers.Valiant.event

        roundTrips(
            "Whenever ~ becomes the target of a spell or ability you control for the first time " +
                "each turn, draw a card."
        )
        roundTrips(
            "Whenever ~ becomes the target of a spell or ability you control for the first time " +
                "each turn, ~ gets +0/+2 until end of turn."
        )
    }

    // The id is not in the text, so it must not stop a card's own ability from printing — the one
    // field the fail-closed comparison deliberately exempts.
    "an ability's arbitrary id does not stop it printing" {
        val theirs = TriggeredAbility(
            id = AbilityId("ability_1"),
            trigger = SdkTriggers.EntersBattlefield.event,
            binding = SdkTriggers.EntersBattlefield.binding,
            effect = Effects.DrawCards(1),
        )

        Grammar.abilityLine.printLine(
            CardFragment(script = CardScript(triggeredAbilities = listOf(theirs)))
        ) shouldBe "When ~ enters, draw a card."
    }

    // ---------------------------------------------------------------------------------------
    // Batch triggers — CR 603.2c's "one or more"
    // ---------------------------------------------------------------------------------------

    fun declines(line: String) {
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    // The three controller scopes are the three facades `dsl.Triggers` publishes over one event, so
    // this is the assertion that the clause in the sentence and the field on the filter are the
    // same fact. "You control" is the *absent* predicate — see [Filters.pluralSubject].
    "a batch subject's controller clause names the facade the SDK publishes" {
        ability("Whenever one or more creatures you control die, draw a card.").trigger shouldBe
            SdkTriggers.OneOrMoreCreaturesYouControlDie().event
        ability("Whenever one or more creatures die, draw a card.").trigger shouldBe
            SdkTriggers.OneOrMoreCreaturesDie().event
        ability("Whenever one or more creatures your opponents control die, draw a card.").trigger shouldBe
            SdkTriggers.OneOrMoreCreaturesAnOpponentControlsDie().event
    }

    // The cap is a rider on the *ability*, so one rule reaches every trigger family rather than
    // every family growing a row. Until it existed the fail-closed reconstruction refused to print
    // a capped ability at all, so every card carrying the rider declined.
    "the once-each-turn rider caps any trigger the grammar can read" {
        ability("When ~ enters, draw a card. This ability triggers only once each turn.")
            .oncePerTurn shouldBe true
        ability(
            "Whenever one or more other creatures die, draw a card. " +
                "This ability triggers only once each turn."
        ).oncePerTurn shouldBe true

        roundTrips("When ~ enters, draw a card. This ability triggers only once each turn.")
    }

    // This band's two write-offs, asserted so they stay declines rather than drifting into a
    // half-reading. Attacks: `YouAttackEvent` cannot carry the "attack **a player**" narrowing that
    // eight of its printed lines have, so the two English sentences would collapse to one model.
    // "During your turn" is a `triggerRestriction`, a clause nobody has written yet.
    "the band's write-offs decline rather than being approximated" {
        declines("Whenever one or more Merfolk you control attack, draw a card.")
        declines("Whenever one or more cards leave your graveyard during your turn, draw a card.")
    }

    "every batch trigger rule prints what it parses" {
        listOf(
            "Whenever one or more creatures you control deal combat damage to a player, draw a card.",
            "Whenever one or more creatures deal combat damage to you, draw a card.",
            "Whenever one or more cards leave your graveyard, draw a card.",
            "Whenever one or more creature cards leave your graveyard, draw a card.",
            "Whenever one or more cards are put into your graveyard from anywhere, draw a card.",
            "Whenever one or more creature cards are put into your graveyard from your library, draw a card.",
            "Whenever one or more creatures you control enter, draw a card.",
            "Whenever one or more other creatures you control enter, draw a card.",
            "Whenever one or more creatures your opponents control enter, draw a card.",
            "Whenever one or more creatures enter, draw a card.",
            "Whenever one or more artifacts you control enter, draw a card.",
            "Whenever one or more creatures you control die, draw a card.",
            "Whenever one or more other creatures die, draw a card.",
            "Whenever one or more creatures your opponents control die, draw a card.",
            "Whenever one or more other creatures you control leave the battlefield without dying, draw a card.",
            "Whenever one or more +1/+1 counters are put on ~, draw a card.",
            "Whenever one or more +1/+1 counters are put on a creature you control, draw a card.",
            "Whenever you put one or more +1/+1 counters on a creature you control, draw a card.",
            "Whenever one or more creatures attack you, draw a card.",
            "Whenever one or more of your opponents are attacked, draw a card.",
        ).forEach { line -> Grammar.abilityLine.printLine(fragment(line)) shouldBe line }
    }

    // ---------------------------------------------------------------------------------------
    // Compound self-triggers — the pairs Oracle joins with "or"
    // ---------------------------------------------------------------------------------------

    // One printed sentence, two abilities: the events are different and the payoff is shared, which
    // is what every hand-written card in the family writes (Queen's Bay Paladin, Ponyback Brigade).
    "an enters-or-dies sentence is the two abilities the goldens carry" {
        val abilities = fragment("When ~ enters or dies, draw a card.").script.triggeredAbilities
        abilities.map { it.trigger } shouldBe
            listOf(SdkTriggers.EntersBattlefield.event, SdkTriggers.Dies.event)
        abilities.map { it.effect } shouldBe List(2) { Effects.DrawCards(1) }
        roundTrips("When ~ enters or dies, draw a card.")
    }

    // Two spellings of one model, so exactly one prints. CR 700.4 defines "dies" as the long form,
    // and the artifact cycle that predates the word spells it out (Ichor Wellspring); fourteen older
    // cards spell "attacks or blocks" with "When" (Mardu Blazebringer, Windscouter).
    "the older spellings of a pair parse and normalize to the printed one" {
        fragment("When ~ enters or is put into a graveyard from the battlefield, draw a card.") shouldBe
            fragment("When ~ enters or dies, draw a card.")
        fragment("When ~ attacks or blocks, draw a card.") shouldBe
            fragment("Whenever ~ attacks or blocks, draw a card.")

        Grammar.abilityLine.printLine(
            fragment("When ~ enters or is put into a graveyard from the battlefield, draw a card.")
        ) shouldBe "When ~ enters or dies, draw a card."
    }

    // The standing finding this band leaves behind. `EventPattern.AnyOf` — `dsl.Triggers.or` — is
    // one ability watching both events, and three cards use it for "enters or is turned face up"
    // while three others in the same family write the two abilities this rule prints. The grammar
    // emits the majority spelling and the differential reports Rakish Scoundrel; a `match` that
    // also accepted the single ability would be two readings of one text.
    "a single AnyOf ability is not what this sentence prints" {
        val anyOf = CardFragment(
            script = CardScript(
                triggeredAbilities = listOf(
                    TriggeredAbility(
                        id = AbilityId("trigger"),
                        trigger = SdkTriggers.or(
                            SdkTriggers.EntersBattlefield,
                            SdkTriggers.TurnedFaceUp,
                        ).event,
                        binding = SdkTriggers.EntersBattlefield.binding,
                        effect = Effects.DrawCards(1),
                    )
                )
            )
        )
        Grammar.abilityLine.printLine(anyOf) shouldBe null
    }

    // The joins deliberately left out: an event with no `TriggerSpec` ("specializes"), and one that
    // is a filtered second event rather than a second self-event ("becomes blocked by a creature").
    "a join over an event the SDK cannot name declines rather than losing half of it" {
        declines("When ~ enters or specializes, draw a card.")
        declines("Whenever ~ blocks or becomes blocked by a creature, draw a card.")
    }

    "every paired trigger rule prints what it parses" {
        listOf(
            "Whenever ~ enters or attacks, draw a card.",
            "Whenever ~ attacks or blocks, draw a card.",
            "When ~ enters or dies, draw a card.",
            "When ~ enters or leaves the battlefield, draw a card.",
            "When ~ enters or is turned face up, draw a card.",
        ).forEach { line -> Grammar.abilityLine.printLine(fragment(line)) shouldBe line }
    }

    // ---------------------------------------------------------------------------------------
    // The join — "When ~ enters and whenever …", two `when` clauses over one payoff
    // ---------------------------------------------------------------------------------------

    // Up the Beanstalk, and the assertion that the rule is the *product* the paired table is not:
    // neither half is spelled here, both come out of the one prefix vocabulary.
    "a join reads both halves out of the trigger vocabulary" {
        val abilities = fragment(
            "When ~ enters and whenever you cast a spell with mana value 5 or greater, draw a card."
        ).script.triggeredAbilities

        abilities.map { it.trigger }.first() shouldBe SdkTriggers.EntersBattlefield.event
        abilities.map { it.trigger }.last().shouldBeInstanceOf<EventPattern.SpellCastEvent>()
        abilities.map { it.effect } shouldBe listOf(Effects.DrawCards(1), Effects.DrawCards(1))
        roundTrips(
            "When ~ enters and whenever you cast a spell with mana value 5 or greater, draw a card."
        )
    }

    // The vocabulary is slotted on *both* sides, so the join is not an enters-trigger rule with a
    // rider — a step trigger, a cast trigger or a cycling trigger opens one just as well. Every
    // line here is a printed card: Crack in Time, Frenzied Fugue, Titans' Vanguard, Esper
    // Sojourners.
    "either half of a join is any trigger the grammar reads" {
        listOf(
            "When ~ enters and at the beginning of your upkeep, draw a card.",
            "When ~ enters and whenever you cast a creature spell, draw a card.",
            "When you cast this spell and whenever ~ attacks, draw a card.",
            "When you cycle ~ and when ~ dies, draw a card.",
            "At the beginning of your upkeep and whenever you cast a creature spell, draw a card.",
        ).forEach { roundTrips(it) }
    }

    // The payoff is the *source* anaphor even where a half's event names an object of its own:
    // Hoarder's Overflow's "put a stash counter on it" is the source under both of its events, and
    // an "it" resolved to a triggering object would mean two different things.
    "a joined payoff reads its anaphor as the source, once, for both halves" {
        val abilities = fragment(
            "When ~ enters and whenever you cast a creature spell, put a +1/+1 counter on it."
        ).script.triggeredAbilities

        // The same effect under both halves, and the same one the single-trigger line denotes —
        // which is the assertion that the join slots the *source* cascade rather than the
        // triggering-object one a lone cast trigger would take.
        abilities.map { it.effect }.distinct() shouldBe
            listOf(ability("When ~ enters, put a +1/+1 counter on it.").effect)
    }

    // One printed form per model, decided by the model. `[enters, dies]` is a pair Oracle contracts
    // into one trigger word, so it prints through the paired table and the join refuses it — in both
    // directions, which is why the guard is on the pair and not on the alternation's order.
    "a pair Oracle contracts into one trigger word never prints as a join" {
        Grammar.abilityLine.printLine(fragment("When ~ enters or dies, draw a card.")) shouldBe
            "When ~ enters or dies, draw a card."
        declines("When ~ enters and when ~ dies, draw a card.")
    }

    // Two identical abilities is not a sentence: it is one trigger line written twice.
    "a join of an event with itself declines" {
        declines("When ~ enters and when ~ enters, draw a card.")
    }
})
