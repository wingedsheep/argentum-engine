package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.GiftKind
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.CastChoiceMade
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CreatePredefinedTokenEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GiftGivenEffect
import com.wingedsheep.sdk.scripting.effects.TakeExtraTurnEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.serialization.CardValidationError
import com.wingedsheep.sdk.serialization.CardValidator
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Unit coverage for the gift DSL surface (CR 702.174).
 *
 * The rules enumerate the whole `[something]` list (CR 702.174d–i) but only two kinds have a card
 * today, so the four unused mappings are pinned here rather than left to the first card that needs
 * one. Also covers the derived enters-ability's shape (CR 702.174b) and the permanent-only guard.
 */
class GiftDslTest : DescribeSpec({

    /** The derived gift ability's rules text. */
    fun CardDefinition.giftAbilityText(): String =
        script.triggeredAbilities.single().descriptionOverride ?: ""


    /** The recipient every gift effect must address: the opponent locked in as the cost was paid. */
    val recipient = EffectTarget.PlayerRef(Player.ChosenOpponent)

    describe("giftEffect maps each GiftKind to its CR-defined effect") {

        it("CARD — the chosen player draws a card (CR 702.174e)") {
            val draw = giftEffect(GiftKind.CARD).shouldBeInstanceOf<DrawCardsEffect>()
            draw.target shouldBe recipient
        }

        it("FOOD — the chosen player creates a Food token (CR 702.174d)") {
            val food = giftEffect(GiftKind.FOOD).shouldBeInstanceOf<CreatePredefinedTokenEffect>()
            food.tokenType shouldBe "Food"
            food.controller shouldBe recipient
        }

        it("TREASURE — the chosen player creates a Treasure token (CR 702.174h)") {
            val treasure = giftEffect(GiftKind.TREASURE).shouldBeInstanceOf<CreatePredefinedTokenEffect>()
            treasure.tokenType shouldBe "Treasure"
            treasure.controller shouldBe recipient
        }

        it("TAPPED_FISH — a tapped 1/1 blue Fish for the chosen player (CR 702.174f)") {
            val fish = giftEffect(GiftKind.TAPPED_FISH).shouldBeInstanceOf<CreateTokenEffect>()
            fish.power shouldBe 1
            fish.toughness shouldBe 1
            fish.colors shouldBe setOf(Color.BLUE)
            fish.creatureTypes shouldBe setOf("Fish")
            fish.tapped shouldBe true
            fish.controller shouldBe recipient
        }

        it("OCTOPUS — an 8/8 blue Octopus for the chosen player (CR 702.174i)") {
            val octopus = giftEffect(GiftKind.OCTOPUS).shouldBeInstanceOf<CreateTokenEffect>()
            octopus.power shouldBe 8
            octopus.toughness shouldBe 8
            octopus.colors shouldBe setOf(Color.BLUE)
            octopus.creatureTypes shouldBe setOf("Octopus")
            octopus.tapped shouldBe false
            octopus.controller shouldBe recipient
        }

        it("EXTRA_TURN — the chosen player takes an extra turn (CR 702.174g)") {
            val extraTurn = giftEffect(GiftKind.EXTRA_TURN).shouldBeInstanceOf<TakeExtraTurnEffect>()
            extraTurn.target shouldBe recipient
        }

        it("covers every kind the rules list, so a new one can't ship unmapped") {
            GiftKind.entries.forEach { kind -> giftEffect(kind).shouldBeInstanceOf<Effect>() }
        }
    }

    describe("gift(kind) on a permanent") {

        val equipment = card("Test Gift Equipment") {
            typeLine = "Artifact — Equipment"
            gift(GiftKind.TAPPED_FISH)
        }

        it("adds the keyword and exactly one derived enters ability (CR 702.174a–b)") {
            equipment.keywordAbilities shouldHaveSize 1
            equipment.keywordAbilities.single() shouldBe KeywordAbility.Gift(GiftKind.TAPPED_FISH)
            equipment.script.triggeredAbilities shouldHaveSize 1
        }

        it("gates the enters ability on the promise as an intervening if (CR 603.4)") {
            val condition = equipment.script.triggeredAbilities.single().triggerCondition
                .shouldBeInstanceOf<CastChoiceMade>()
            condition.slot shouldBe ChoiceSlot.GIFT_PROMISED
        }

        it("closes with the GiftGiven marker so \"whenever you give a gift\" fires (CR 702.174c)") {
            val composite = equipment.script.triggeredAbilities.single().effect
                .shouldBeInstanceOf<CompositeEffect>()
            composite.effects.last().shouldBeInstanceOf<GiftGivenEffect>()
        }

        it("names the permanent the way the printed card does") {
            equipment.giftAbilityText() shouldContain "When this Equipment enters"

            val aura = card("Test Gift Aura") {
                typeLine = "Enchantment — Aura"
                gift(GiftKind.CARD)
            }
            aura.giftAbilityText() shouldContain "When this Aura enters"
        }

        it("falls back to the rule's own wording when the type line isn't set yet") {
            val unordered = card("Test Gift Unordered") {
                gift(GiftKind.CARD)
                typeLine = "Creature — Raccoon"
                power = 1
                toughness = 1
            }
            unordered.giftAbilityText() shouldContain "When this permanent enters"
        }
    }

    describe("the keyword is permanent-only") {

        it("is rejected on an instant, which has no enters trigger to fire (CR 702.174b)") {
            val instant = card("Test Gift Instant") {
                typeLine = "Instant"
                gift(GiftKind.CARD)
            }
            val errors = CardValidator.validate(instant)
                .filterIsInstance<CardValidationError.GiftKeywordOnNonPermanent>()
            errors shouldHaveSize 1
            errors.single().message shouldContain "giftSpell"
        }

        it("accepts a permanent") {
            CardValidator.validate(
                card("Test Gift Enchantment") {
                    typeLine = "Enchantment"
                    gift(GiftKind.FOOD)
                }
            ).filterIsInstance<CardValidationError.GiftKeywordOnNonPermanent>() shouldHaveSize 0
        }
    }
})
