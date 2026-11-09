package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/** Entwine (CR 702.42a): the optional cost is declared once, and it moves the mode count to "all". */
class EntwineDslTest : FunSpec({

    fun entwinedCard(chooseCount: Int, modeCount: Int) = card("Test Entwine") {
        manaCost = "{1}{W}"
        typeLine = "Instant"
        entwine("{3}")
        spell {
            entwineModal(chooseCount) {
                repeat(modeCount) { mode("Mode $it", Effects.GainLife(it + 1)) }
            }
        }
    }

    test("entwine rides the optional-additional-cost rail under its own slot") {
        val cost = entwinedCard(chooseCount = 2, modeCount = 4).keywordAbilities
            .single().shouldBeInstanceOf<KeywordAbility.OptionalAdditionalCost>()

        cost.manaCost.toString() shouldBe "{3}"
        cost.declaredSlot shouldBe ChoiceSlot.ENTWINED
        cost.keyword shouldBe Keyword.ENTWINE
        cost.multi shouldBe false
        cost.isKicker shouldBe false
        cost.description shouldBe "Entwine {3}"
    }

    test("a non-mana entwine cost rides the same slot and prints as Entwine—cost") {
        val sacrificeLands = card("Test Sacrifice Entwine") {
            manaCost = "{5}{B}"
            typeLine = "Instant"
            entwine(Costs.additional.SacrificePermanent(GameObjectFilter.Land, count = 3))
            spell {
                entwineModal {
                    mode("Gain 1", Effects.GainLife(1))
                    mode("Gain 2", Effects.GainLife(2))
                }
            }
        }
        val cost = sacrificeLands.keywordAbilities.single().shouldBeInstanceOf<KeywordAbility.OptionalAdditionalCost>()

        cost.manaCost shouldBe null
        cost.additionalCost shouldBe Costs.additional.SacrificePermanent(GameObjectFilter.Land, count = 3)
        cost.declaredSlot shouldBe ChoiceSlot.ENTWINED
        cost.keyword shouldBe Keyword.ENTWINE
        cost.description shouldBe "Entwine—${cost.additionalCost!!.description}"
    }

    test("choose two, or all four when entwined — both bounds ride the same conditional") {
        val modal = entwinedCard(chooseCount = 2, modeCount = 4).script.spellEffect
            .shouldBeInstanceOf<ModalEffect>()

        val expected = DynamicAmount.Conditional(Conditions.WasEntwined, DynamicAmount.Fixed(4), DynamicAmount.Fixed(2))
        modal.dynamicChooseCount shouldBe expected
        modal.dynamicMinChooseCount shouldBe expected
        modal.minChooseCount shouldBe 2
        modal.chooseCount shouldBe 4
        modal.additionalManaCostPerExtraMode shouldBe null
    }

    test("choose one, or both when entwined — the classic printing") {
        val modal = entwinedCard(chooseCount = 1, modeCount = 2).script.spellEffect
            .shouldBeInstanceOf<ModalEffect>()

        val expected = DynamicAmount.Conditional(Conditions.WasEntwined, DynamicAmount.Fixed(2), DynamicAmount.Fixed(1))
        modal.dynamicChooseCount shouldBe expected
        modal.dynamicMinChooseCount shouldBe expected
        modal.minChooseCount shouldBe 1
    }

    test("choosing all modes must differ from the printed count") {
        shouldThrow<IllegalArgumentException> { entwinedCard(chooseCount = 2, modeCount = 2) }
    }

    test("teamworkModal is the same shape with a count of one and the teamwork condition") {
        val c = card("Test Teamwork") {
            manaCost = "{1}{W}"
            typeLine = "Sorcery"
            spell {
                teamworkModal {
                    mode("A", Effects.GainLife(1))
                    mode("B", Effects.GainLife(2))
                }
            }
        }
        val modal = c.script.spellEffect.shouldBeInstanceOf<ModalEffect>()
        val expected = DynamicAmount.Conditional(Conditions.TeamworkWasPaid, DynamicAmount.Fixed(2), DynamicAmount.Fixed(1))
        modal.dynamicChooseCount shouldBe expected
        modal.dynamicMinChooseCount shouldBe expected
        modal.chooseCount shouldBe 2
        modal.minChooseCount shouldBe 1
    }
})
