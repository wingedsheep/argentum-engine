package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.RemoveCardType
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thassa, Deep-Dwelling
 * {3}{U}
 * Legendary Enchantment Creature — God
 * 6/5
 * Indestructible
 * As long as your devotion to blue is less than five, Thassa isn't a creature.
 * At the beginning of your end step, exile up to one other target creature you control, then return
 * that card to the battlefield under your control.
 * {3}{U}: Tap another target creature.
 *
 * The devotion clause is the THB God template (Erebos, Heliod). The end-step blink is Nephalia
 * Smuggler's exile-then-return with `controllerOverride = Controller` (a borrowed creature comes back
 * under your control for good — ruling), on Conjurer's Closet's end-step trigger with an optional
 * "up to one" target. A token exiled this way ceases to exist and doesn't return (ruling). The tap
 * ability is Marshal of Zhalfir's.
 */
val ThassaDeepDwelling = card("Thassa, Deep-Dwelling") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Enchantment Creature — God"
    power = 6
    toughness = 5
    oracleText = "Indestructible\n" +
        "As long as your devotion to blue is less than five, Thassa isn't a creature.\n" +
        "At the beginning of your end step, exile up to one other target creature you control, then return " +
        "that card to the battlefield under your control.\n" +
        "{3}{U}: Tap another target creature."

    keywords(Keyword.INDESTRUCTIBLE)

    staticAbility {
        condition = Conditions.CompareAmounts(DynamicAmounts.devotionTo(Color.BLUE), ComparisonOperator.LT, 5)
        ability = RemoveCardType("CREATURE", GroupFilter.source())
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        val creature = target(TargetFilter.OtherCreatureYouControl, optional = true)
        effect = Effects.Exile(creature) then
            Effects.Move(
                creature,
                Zone.BATTLEFIELD,
                controllerOverride = EffectTarget.Controller,
            )
        description = "At the beginning of your end step, exile up to one other target creature you control, " +
            "then return that card to the battlefield under your control."
    }

    activatedAbility {
        cost = Costs.Mana("{3}{U}")
        val creature = target(TargetFilter.OtherCreature)
        effect = Effects.Tap(creature)
        description = "{3}{U}: Tap another target creature."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "71"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c83ed3e0-82d0-4410-a6ca-b0f923eadf83.jpg?1783931576"
        ruling(
            "2020-01-24",
            "When the card returns to the battlefield, it will be a new object with no connection to the card " +
                "that was exiled. Auras attached to the exiled creature will be put into their owners' graveyards. " +
                "Any Equipment will become unattached and remain on the battlefield. Any counters on the exiled " +
                "creature will cease to exist."
        )
        ruling("2020-01-24", "If a token is exiled this way, it will cease to exist and won't return to the battlefield.")
        ruling(
            "2020-01-24",
            "If you gain control of a creature \"until end of turn,\" you'll control it during your end step."
        )
        ruling(
            "2020-01-24",
            "When an effect returns the exiled card \"under your control,\" you control it indefinitely after that."
        )
        ruling(
            "2020-01-24",
            "If a God stops being a creature, it loses the type creature and the creature type God. It continues " +
                "to be a legendary enchantment."
        )
        ruling(
            "2020-01-24",
            "The type-changing ability that can make a God not be a creature functions only on the battlefield. " +
                "It's always a creature card in other zones, regardless of your devotion to its color. It's always " +
                "a creature spell while it's on the stack."
        )
    }
}
