package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.EvidenceCollectedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.collectEvidence
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Collect evidence N (CR 701.59, Murders at Karlov Manor) end to end.
 *
 * > **701.59a** To "collect evidence N" means to exile any number of cards from your graveyard with
 * > total mana value N or greater.
 * > **701.59b** If a player is given the choice to collect evidence but is unable to exile cards
 * > with total mana value N or greater from their graveyard … they can't choose to collect evidence.
 * > **701.59c** A spell that has an ability that allows a player to collect evidence as an
 * > additional cost … may have another ability that refers to whether evidence was collected. These
 * > abilities are linked. See rule 607.
 *
 * The mechanic appears in four contexts that all mean the same thing, so the payable thing is one
 * shared `CostAtom.CollectEvidence` and one shared payment implementation
 * (`CollectEvidenceResolver`). These tests pin every context against the rules above, plus the two
 * properties that make the design correct: that the *threshold is a mana-value sum, never a card
 * count*, and that "evidence was collected" stays a **separate fact** from kicked and bargained on
 * the shared optional-additional-cost rail.
 */
class CollectEvidenceScenarioTest : ScenarioTestBase() {

    // --- Graveyard fodder, priced so a test can hit a threshold exactly or miss it -------------

    /** Mana value 1. Three of these total 3 — enough for evidence 3, never for evidence 6. */
    private val pebble = card("Test Pebble") {
        manaCost = "{1}"
        typeLine = "Artifact"
    }

    /** Mana value 3. */
    private val relic = card("Test Relic") {
        manaCost = "{2}{B}"
        typeLine = "Artifact"
    }

    /** Mana value 6 — reaches evidence 6 on its own. */
    private val colossus = card("Test Colossus") {
        manaCost = "{6}"
        typeLine = "Artifact"
    }

    /**
     * Mana value **0** (CR 202.3b — a land has no mana cost). The card that proves the threshold is
     * a mana-value sum and not a card count: a graveyard of these is never enough evidence.
     */
    private val wastes = card("Test Wastes") {
        typeLine = "Land"
    }

    private val bear = card("Test Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    // --- Cards under test, one per printed shape ----------------------------------------------

    // Vitu-Ghazi Inspector's shape: optional cast cost + intervening-if enters trigger.
    private val inspector = card("Evidence Inspector") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Elf Detective"
        power = 1
        toughness = 3
        collectEvidence(6)
        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            triggerCondition = Conditions.WasEvidenceCollected
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
        }
    }

    // Crimestopper Sprite's shape: the trigger fires either way, only the rider is gated.
    private val sprite = card("Evidence Sprite") {
        manaCost = "{2}{U}"
        typeLine = "Creature — Faerie Detective"
        power = 2
        toughness = 2
        collectEvidence(6)
        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            effect = Effects.Composite(
                Effects.GainLife(1),
                ConditionalEffect(
                    condition = Conditions.WasEvidenceCollected,
                    effect = Effects.GainLife(5),
                ),
            )
        }
    }

    // Bite Down on Crime's shape: the collect-evidence cast branch is priced {2} cheaper.
    private val discount = card("Evidence Discount") {
        manaCost = "{3}{G}"
        typeLine = "Creature — Giant"
        power = 4
        toughness = 4
        collectEvidence(6)
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.SelfCast,
                modification = CostModification.ReduceGeneric(2),
                gating = CostGating.OnlyIf(Conditions.WasEvidenceCollected),
            )
        }
    }

    // Sample Collector's shape: an *effect*, with a reflexive "when you do" that targets.
    private val collector = card("Evidence Collector") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Troll Detective"
        power = 2
        toughness = 3
        triggeredAbility {
            trigger = Triggers.Attacks
            effect = ReflexiveTriggerEffect(
                action = Effects.CollectEvidence(3),
                optional = true,
                reflexiveEffect = Effects.AddCounters(
                    Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)
                ),
                reflexiveTargetRequirements = listOf(
                    TargetCreature(filter = TargetFilter.Creature.youControl())
                ),
            )
        }
    }

    // Forensic Researcher's shape: an activated-ability cost, unlinked.
    private val examiner = card("Evidence Examiner") {
        manaCost = "{2}{U}"
        typeLine = "Creature — Merfolk Detective"
        power = 1
        toughness = 3
        activatedAbility {
            cost = Costs.Composite(Costs.Tap, Costs.CollectEvidence(3))
            effect = Effects.GainLife(4)
            description = "You gain 4 life."
        }
    }

    // Surveillance Monitor's shape: the "whenever you collect evidence" payoff.
    private val monitor = card("Evidence Monitor") {
        manaCost = "{3}{U}"
        typeLine = "Creature — Vedalken Detective"
        power = 3
        toughness = 3
        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            effect = MayEffect(Effects.CollectEvidence(4))
        }
        triggeredAbility {
            trigger = Triggers.WheneverYouCollectEvidence
            effect = Effects.GainLife(7)
        }
    }

    // Controls for the "separate fact" tests — same rail, different slots.
    private val kickerBear = card("Evidence Kicker Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.OptionalAdditionalCost(ManaCost.parse("{1}")))
        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            triggerCondition = Conditions.WasEvidenceCollected
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
        }
    }

    private val evidenceBearReadsKicked = card("Evidence Bear Reads Kicked") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        collectEvidence(6)
        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            triggerCondition = WasKicked
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
        }
    }

    /** The battlefield permanent named [name], as a component container. */
    private fun TestGame.containerOf(name: String): ComponentContainer =
        state.getEntity(findPermanent(name) ?: error("'$name' is not on the battlefield"))
            ?: error("'$name' has no entity")

    /** Activate [name]'s only activated ability, letting the engine auto-pay the mana. */
    private fun TestGame.activateOnlyAbility(playerNumber: Int, name: String): ExecutionResult {
        val sourceId = findPermanent(name) ?: error("'$name' is not on the battlefield")
        val abilityId = cardRegistry.getCard(name)
            ?.activatedAbilities?.firstOrNull()?.id
            ?: error("'$name' has no activated ability")
        return execute(
            ActivateAbility(
                playerId = if (playerNumber == 1) player1Id else player2Id,
                sourceId = sourceId,
                abilityId = abilityId,
                paymentStrategy = PaymentStrategy.AutoPay,
            )
        )
    }

    private fun ComponentContainer.declaredEvidence(): Boolean =
        get<CastChoicesComponent>()?.chosen?.containsKey(ChoiceSlot.EVIDENCE_COLLECTED) == true

    private fun ComponentContainer.plusOneCounters(): Int =
        get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        cardRegistry.register(pebble)
        cardRegistry.register(relic)
        cardRegistry.register(colossus)
        cardRegistry.register(wastes)
        cardRegistry.register(bear)
        cardRegistry.register(inspector)
        cardRegistry.register(sprite)
        cardRegistry.register(discount)
        cardRegistry.register(collector)
        cardRegistry.register(examiner)
        cardRegistry.register(monitor)
        cardRegistry.register(kickerBear)
        cardRegistry.register(evidenceBearReadsKicked)

        // ---------------------------------------------------------------------------------------
        // CR 701.59a — exile any number of cards with total mana value N or greater
        // ---------------------------------------------------------------------------------------

        test("collecting evidence exiles the chosen cards and satisfies the linked condition") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Evidence Inspector")
                .withCardInGraveyard(1, "Test Colossus")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellCollectingEvidence(1, "Evidence Inspector", "Test Colossus")
                .error shouldBe null
            game.resolveStack()

            game.isInExile(1, "Test Colossus") shouldBe true
            val permanent = game.containerOf("Evidence Inspector")
            permanent.declaredEvidence() shouldBe true
            permanent.plusOneCounters() shouldBe 2
        }

        test("several cheap cards may combine to reach the threshold") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Evidence Inspector")
                .withCardInGraveyard(1, "Test Relic")
                .withCardInGraveyard(1, "Test Relic")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // 3 + 3 = 6 exactly.
            game.castSpellCollectingEvidence(1, "Evidence Inspector", "Test Relic", "Test Relic")
                .error shouldBe null
            game.resolveStack()

            game.containerOf("Evidence Inspector").plusOneCounters() shouldBe 2
        }

        test("exiling more than the threshold is legal — the constraint is a floor, not an exact sum") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Evidence Inspector")
                .withCardInGraveyard(1, "Test Colossus")
                .withCardInGraveyard(1, "Test Relic")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // 6 + 3 = 9 for a threshold of 6. Overpaying is the player's right.
            game.castSpellCollectingEvidence(
                1, "Evidence Inspector", "Test Colossus", "Test Relic"
            ).error shouldBe null
            game.resolveStack()

            game.isInExile(1, "Test Colossus") shouldBe true
            game.isInExile(1, "Test Relic") shouldBe true
            game.containerOf("Evidence Inspector").plusOneCounters() shouldBe 2
        }

        test("an under-total selection is rejected — the spell is not cast and nothing is exiled") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Evidence Inspector")
                .withCardInGraveyard(1, "Test Relic")
                .withCardInGraveyard(1, "Test Colossus")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // 3 alone doesn't reach 6, even though the graveyard as a whole could.
            game.castSpellCollectingEvidence(1, "Evidence Inspector", "Test Relic")
                .error.shouldNotBeNull()

            game.isInHand(1, "Evidence Inspector") shouldBe true
            game.isInGraveyard(1, "Test Relic") shouldBe true
            game.isInExile(1, "Test Relic") shouldBe false
        }

        // ---------------------------------------------------------------------------------------
        // CR 701.59b — an unreachable threshold is not a choice a player may make
        // ---------------------------------------------------------------------------------------

        test("with an empty graveyard the collect-evidence cast is never affordable") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Evidence Inspector")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val casts = game.getLegalActions(1)
                .filter { it.description.contains("Evidence Inspector") }

            // The branch is listed but unaffordable — the engine's uniform treatment of an
            // unpayable cost, and the same shape bargain uses when there is nothing to sacrifice.
            casts.filter { it.description.contains("Collect evidence") }
                .none { it.isAffordable } shouldBe true
            // The plain cast is unaffected — only the collection is impossible.
            casts.filterNot { it.description.contains("Collect evidence") }
                .any { it.isAffordable } shouldBe true

            // CR 701.59b bites for real at submission: an empty graveyard can pay nothing.
            game.castSpellCollectingEvidence(1, "Evidence Inspector").error.shouldNotBeNull()
            game.isInHand(1, "Evidence Inspector") shouldBe true
        }

        test("a graveyard of lands is never enough evidence — mana value 0 contributes nothing") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Evidence Inspector")
                .withCardInGraveyard(1, "Test Wastes")
                .withCardInGraveyard(1, "Test Wastes")
                .withCardInGraveyard(1, "Test Wastes")
                .withCardInGraveyard(1, "Test Wastes")
                .withCardInGraveyard(1, "Test Wastes")
                .withCardInGraveyard(1, "Test Wastes")
                .withCardInGraveyard(1, "Test Wastes")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // Seven cards, total mana value 0. Card *count* would say yes; the rule says no.
            game.getLegalActions(1)
                .filter { it.description.contains("Evidence Inspector") }
                .filter { it.description.contains("Collect evidence") }
                .none { it.isAffordable } shouldBe true

            // And exiling all seven still cannot pay for it.
            game.castSpellCollectingEvidence(
                1, "Evidence Inspector",
                "Test Wastes", "Test Wastes", "Test Wastes", "Test Wastes",
                "Test Wastes", "Test Wastes", "Test Wastes",
            ).error.shouldNotBeNull()
            game.isInExile(1, "Test Wastes") shouldBe false
        }

        test("lands are still legal selections alongside real cards") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Evidence Inspector")
                .withCardInGraveyard(1, "Test Colossus")
                .withCardInGraveyard(1, "Test Wastes")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // The Colossus carries the whole threshold; the land rides along contributing 0.
            game.castSpellCollectingEvidence(
                1, "Evidence Inspector", "Test Colossus", "Test Wastes"
            ).error shouldBe null
            game.resolveStack()

            game.isInExile(1, "Test Wastes") shouldBe true
            game.containerOf("Evidence Inspector").plusOneCounters() shouldBe 2
        }

        test("an activated ability with a collect-evidence cost is unactivatable below the threshold") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "Evidence Examiner")
                .withCardInGraveyard(1, "Test Pebble")
                .withCardInGraveyard(1, "Test Pebble")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // Two mana value 1 cards = 2, short of 3. The action is surfaced but unaffordable —
            // the same way every other unpayable activated ability is — and CR 701.59b is enforced
            // by refusing the activation, not merely by hiding it.
            game.getLegalActions(1)
                .filter { it.description.contains("You gain 4 life") }
                .none { it.isAffordable } shouldBe true

            val life = game.getLifeTotal(1)
            game.activateOnlyAbility(1, "Evidence Examiner").error.shouldNotBeNull()
            game.getLifeTotal(1) shouldBe life
            game.isInGraveyard(1, "Test Pebble") shouldBe true
        }

        test("the same ability becomes activatable once the graveyard reaches the threshold") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "Evidence Examiner")
                .withCardInGraveyard(1, "Test Relic")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val life = game.getLifeTotal(1)
            game.activateOnlyAbility(1, "Evidence Examiner").error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe life + 4
            game.isInExile(1, "Test Relic") shouldBe true
        }

        test("the reflexive 'you may collect evidence' is not offered when the graveyard can't pay") {
            val game = scenario()
                .withPlayers("Attacker", "Defender")
                .withCardOnBattlefield(1, "Evidence Collector")
                .withCardInGraveyard(1, "Test Pebble")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Evidence Collector" to 2))
            game.resolveStack()

            // CR 701.59b — the option is absent, not offered and refused, so no decision is pending
            // and the attacker gained no counter.
            game.state.pendingDecision shouldBe null
            game.containerOf("Evidence Collector").plusOneCounters() shouldBe 0
        }

        // ---------------------------------------------------------------------------------------
        // CR 701.59c / 607 — the linkage, and what it is *not*
        // ---------------------------------------------------------------------------------------

        test("declining the collection leaves the linked condition false") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Evidence Inspector")
                .withCardInGraveyard(1, "Test Colossus")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Evidence Inspector").error shouldBe null
            game.resolveStack()

            // Nothing exiled, and per CR 603.4 the intervening-if trigger never went on the stack.
            game.isInGraveyard(1, "Test Colossus") shouldBe true
            val permanent = game.containerOf("Evidence Inspector")
            permanent.declaredEvidence() shouldBe false
            permanent.plusOneCounters() shouldBe 0
        }

        test("an unconditional trigger still fires when evidence wasn't collected — only the rider is gated") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Evidence Sprite")
                .withCardInGraveyard(1, "Test Colossus")
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val life = game.getLifeTotal(1)
            game.castSpell(1, "Evidence Sprite").error shouldBe null
            game.resolveStack()

            // The base clause resolved (1 life); the gated rider (5 more) did not.
            game.getLifeTotal(1) shouldBe life + 1
        }

        test("the same trigger's rider applies when evidence was collected") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Evidence Sprite")
                .withCardInGraveyard(1, "Test Colossus")
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val life = game.getLifeTotal(1)
            game.castSpellCollectingEvidence(1, "Evidence Sprite", "Test Colossus")
                .error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe life + 6
        }

        test("a kicked spell does not read as having collected evidence") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Evidence Kicker Bear")
                .withCardInGraveyard(1, "Test Colossus")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val kickerCard = game.state.getHand(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Evidence Kicker Bear"
            }
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = kickerCard,
                    declaredCostSlot = ChoiceSlot.KICKED,
                    paymentStrategy = PaymentStrategy.AutoPay,
                )
            ).error shouldBe null
            game.resolveStack()

            // Same rail, different slot — the kicker payment must not satisfy WasEvidenceCollected.
            game.containerOf("Evidence Kicker Bear").plusOneCounters() shouldBe 0
        }

        test("collecting evidence does not read as having kicked the spell") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Evidence Bear Reads Kicked")
                .withCardInGraveyard(1, "Test Colossus")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellCollectingEvidence(1, "Evidence Bear Reads Kicked", "Test Colossus")
                .error shouldBe null
            game.resolveStack()

            game.containerOf("Evidence Bear Reads Kicked").plusOneCounters() shouldBe 0
        }

        // ---------------------------------------------------------------------------------------
        // The cost-reduction shape — the reduction is priced against the cast branch
        // ---------------------------------------------------------------------------------------

        test("the collect-evidence cast branch costs {2} less") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Evidence Discount")
                .withCardInGraveyard(1, "Test Colossus")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // Printed {3}{G} = 4 mana, and only two lands are untapped — castable only via the
            // reduced ({1}{G}) collect-evidence branch.
            game.castSpellCollectingEvidence(1, "Evidence Discount", "Test Colossus")
                .error shouldBe null
            game.resolveStack()

            game.findPermanent("Evidence Discount").shouldNotBeNull()
        }

        test("without collecting evidence the same spell is unaffordable at two lands") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Evidence Discount")
                .withCardInGraveyard(1, "Test Colossus")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val branches = game.getLegalActions(1)
                .filter { it.description.contains("Evidence Discount") }

            // The reduced branch is labelled by the mechanic, not "(Kicked)", and is the only one
            // castable off two lands; the plain {3}{G} cast is offered but unaffordable.
            val collectBranch = branches
                .first { it.description.contains("Collect evidence 6") }
            collectBranch.isAffordable shouldBe true

            val plainCast = branches.filterNot { it.description.contains("Collect evidence") }
            plainCast.none { it.isAffordable } shouldBe true
        }

        // ---------------------------------------------------------------------------------------
        // The "whenever you collect evidence" payoff — one event from every context
        // ---------------------------------------------------------------------------------------

        test("a collection made as a cast cost fires 'whenever you collect evidence'") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "Evidence Monitor")
                .withCardInHand(1, "Evidence Inspector")
                .withCardInGraveyard(1, "Test Colossus")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val life = game.getLifeTotal(1)
            game.castSpellCollectingEvidence(1, "Evidence Inspector", "Test Colossus")
                .error shouldBe null
            game.resolveStack()

            // The payoff is on a *different* permanent than the collector — the event is global.
            game.getLifeTotal(1) shouldBe life + 7
        }

        test("a collection made as an activated-ability cost fires the same payoff") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "Evidence Monitor")
                .withCardOnBattlefield(1, "Evidence Examiner")
                .withCardInGraveyard(1, "Test Relic")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val life = game.getLifeTotal(1)
            game.activateOnlyAbility(1, "Evidence Examiner").error shouldBe null
            game.resolveStack()

            // 4 from the ability itself + 7 from the payoff.
            game.getLifeTotal(1) shouldBe life + 11
        }

        test("declining a collection fires no payoff") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "Evidence Monitor")
                .withCardInHand(1, "Evidence Inspector")
                .withCardInGraveyard(1, "Test Colossus")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val life = game.getLifeTotal(1)
            game.castSpell(1, "Evidence Inspector").error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe life
        }

        test("the emitted event carries the threshold and the total actually exiled") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Evidence Inspector")
                .withCardInGraveyard(1, "Test Colossus")
                .withCardInGraveyard(1, "Test Relic")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val result = game.castSpellCollectingEvidence(
                1, "Evidence Inspector", "Test Colossus", "Test Relic"
            )
            result.error shouldBe null

            val event = result.events.filterIsInstance<EvidenceCollectedEvent>()
                .firstOrNull().shouldNotBeNull()
            // `value` is the requirement (6); `totalManaValue` is what was actually spent (9).
            event.value shouldBe 6
            event.totalManaValue shouldBe 9
            event.exiledCards.size shouldBe 2
        }
    }
}
