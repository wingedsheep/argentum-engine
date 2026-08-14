package com.wingedsheep.mtg.sets

import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.costs.manaCostOrNull
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The equip keyword and its "Equip [quality]" variants (CR 702.6a / 702.6c).
 *
 * CR 702.6a defines equip as "[Cost]: Attach this permanent to target creature you control.
 * Activate only as a sorcery." CR 702.6c allows a further restriction — "Equip [quality]" /
 * "Equip [quality] creature" — which "may legally target only a creature that's controlled by the
 * player activating the ability and that has the chosen quality."
 *
 * Three catalog-wide invariants follow, and each has been violated in this repo before:
 *
 *  1. **Every equip ability is flagged.** A quality-restricted equip authored as a bare
 *     `activatedAbility { }` is a real equip ability that the engine cannot see: everything that
 *     keys off `ActivatedAbility.isEquipAbility` — Forge Anew's free first equip, Eowyn's equip
 *     discount, Leonin Shikari's instant-speed-equip permission — silently skips it.
 *  2. **Every equip ability targets a creature *you control*.** The quality narrows the target
 *     set; it never widens it past the controller scope CR 702.6c states.
 *  3. **Every mana-cost equip ability renders as its printed line**, from `equipQuality` plus the
 *     effective cost — not from a frozen `descriptionOverride` that a cost reduction can't rewrite.
 *     Non-mana equip costs ("Equip—Pay 3 life") can't be discounted and may keep an override.
 *
 * All three are stated as properties over the catalog rather than as a list of card names, so a
 * new Equipment never needs this file edited.
 *
 * Scoped to abilities already flagged `isEquipAbility` — a card whose attach happens on an ETB
 * trigger (Pirate's Cutlass, Super Suit) or a loyalty ability (The Aetherspark) is not an equip
 * ability and is correctly outside this.
 */
class EquipQualityVariantTest : FunSpec({

    val equipAbilities = MtgSetCatalog.all
        .flatMap { set -> set.cards }
        .flatMap { card -> card.script.activatedAbilities.filter { it.isEquipAbility }.map { card to it } }

    test("every equip ability is sorcery-speed and targets a single creature you control") {
        equipAbilities.size shouldNotBe 0
        assertSoftly {
            for ((card, ability) in equipAbilities) {
                withClue("${card.name}: ${ability.description}") {
                    ability.timing shouldBe TimingRule.SorcerySpeed
                    ability.targetRequirements.size shouldBe 1
                    val requirement = ability.targetRequirements.single()
                    withClue("equip targets a permanent, so the requirement is a TargetObject") {
                        (requirement is TargetObject) shouldBe true
                    }
                    val filter = (requirement as TargetObject).filter
                    withClue("CR 702.6c: an equip ability may target only a creature you control") {
                        filter.baseFilter.controllerPredicate shouldBe ControllerPredicate.ControlledByYou
                    }
                }
            }
        }
    }

    test("every equip ability renders as its printed 'Equip [quality] [cost]' line") {
        // The regression this replaces a hardcoded card list with: five cards hand-rolled a
        // restricted equip as a bare `activatedAbility { }` carrying a `descriptionOverride`
        // ("Equip Human {1}"), which both left `isEquipAbility` unset and froze the cost — a static
        // string can't be rewritten when Eowyn or Forge Anew discounts the activation.
        // `ActivatedAbility.describeWithCost` now renders the printed line from `equipQuality` plus
        // the *effective* cost, so an override on a flagged equip ability is by definition the stale
        // hand-rolled shape.
        assertSoftly {
            for ((card, ability) in equipAbilities) {
                withClue("${card.name}: ${ability.description}") {
                    withClue("every equip ability names the keyword it is") {
                        ability.description.contains("Equip") shouldBe true
                    }
                    if (ability.cost.manaCostOrNull != null) {
                        // The exemption is narrow on purpose. A *mana* equip cost is exactly what
                        // Eowyn and Forge Anew rewrite, so freezing it in a string is the bug; a
                        // non-mana cost can't be discounted and may need card-specific naming
                        // (Dark Knight's Greatsword prints "Chaosbringer — Equip—Pay 3 life").
                        withClue("a mana equip cost must stay live — no frozen override") {
                            ability.descriptionOverride shouldBe null
                        }
                        // "Equip {3}" (CR 702.6a) / "Equip Human {1}" (CR 702.6c).
                        val expectedPrefix = "Equip ${ability.equipQuality?.let { "$it " } ?: ""}"
                        withClue("renders as its printed keyword line") {
                            ability.description.startsWith(expectedPrefix) shouldBe true
                        }
                    }
                }
            }
        }
    }

    test("a target-restricted equip declares its quality — it is never restricted but unlabelled") {
        // Keyed off the *filter*, not the prompt label: a hand-rolled equip picks its own label
        // ("target Wizard you control"), so only the filter says reliably whether the ability is
        // restricted. Anything narrower than plain creature-you-control is a CR 702.6c variant and
        // must go through the facade, which is what keeps the printed wording, `equipQuality` and
        // the prompt label in agreement. This is the assertion that caught Thinking Cap and
        // Wizard's Staff still hand-rolling the shape.
        val restricted = equipAbilities.filter { (_, ability) ->
            (ability.targetRequirements.singleOrNull() as? TargetObject)?.filter?.baseFilter != PLAIN_EQUIP_FILTER
        }
        withClue("the catalog has quality-restricted equips to assert about") {
            restricted.size shouldNotBe 0
        }
        assertSoftly {
            for ((card, ability) in restricted) {
                withClue("${card.name}: ${ability.description}") {
                    val quality = ability.equipQuality
                    withClue("a narrowed target filter means a quality variant") {
                        quality shouldNotBe null
                    }
                    withClue("the prompt label is built from the quality") {
                        ability.targetRequirements.singleOrNull()?.id shouldBe "$quality creature you control"
                    }
                }
            }
        }
    }
}) {
    private companion object {
        /** The target filter of a plain, unrestricted equip (CR 702.6a). */
        val PLAIN_EQUIP_FILTER = TargetFilter.CreatureYouControl.baseFilter
    }
}
