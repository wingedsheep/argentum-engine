package com.wingedsheep.mtg.sets.definitions.ddp.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Veteran Warleader {1}{G}{W} — Duel Decks: Zendikar vs. Eldrazi #27 (first printing; Battle for Zendikar #221 is a [Printing] row)
 * Creature — Human Soldier Ally, power/toughness star/star
 *
 * "Veteran Warleader's power and toughness are each equal to the number of creatures you control.
 *  Tap another untapped Ally you control: This creature gains your choice of first strike, vigilance,
 *  or trample until end of turn."
 *
 * The characteristic-defining P/T is [DynamicAmounts.creaturesYouControl] (it counts itself on the
 * battlefield). The cost taps any *other* untapped Ally permanent you control — Warleader is an Ally
 * itself, hence `excludeSelf = true` — and doesn't use {T}, so summoning sickness doesn't matter for
 * either creature. The keyword is chosen on resolution, not on activation, which is what
 * [Effects.ChooseAction] does.
 */
val VeteranWarleader = card("Veteran Warleader") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Human Soldier Ally"
    power = 0
    toughness = 0
    oracleText = "Veteran Warleader's power and toughness are each equal to the number of creatures you control.\n" +
        "Tap another untapped Ally you control: This creature gains your choice of first strike, vigilance, or trample until end of turn."

    dynamicStats(DynamicAmounts.creaturesYouControl())

    activatedAbility {
        cost = Costs.TapPermanents(
            count = 1,
            filter = GameObjectFilter.Permanent.withSubtype(Subtype.ALLY),
            excludeSelf = true,
        )
        effect = Effects.ChooseAction(listOf(
            EffectChoice(
                label = "This creature gains first strike until end of turn",
                effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self),
            ),
            EffectChoice(
                label = "This creature gains vigilance until end of turn",
                effect = Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.Self),
            ),
            EffectChoice(
                label = "This creature gains trample until end of turn",
                effect = Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self),
            ),
        ))
        description = "Tap another untapped Ally you control: This creature gains your choice of first strike, vigilance, or trample until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "27"
        artist = "Josu Hernaiz"
        imageUri = "https://cards.scryfall.io/normal/front/5/7/577d6061-c6b9-43e6-9832-6a00f3eb734e.jpg?1783938251"
        ruling("2015-08-25", "The ability that defines Veteran Warleader's power and toughness works in all zones, not just the battlefield. If Veteran Warleader is on the battlefield, it will count itself.")
        ruling("2015-08-25", "To activate the last ability, you can tap any other untapped Ally you control, including one that hasn't been under your control since the beginning of your most recent turn. (Note that ability doesn't use the tap symbol.)")
        ruling("2015-08-25", "You choose which ability Veteran Warleader gains as the last ability resolves, not as you activate it.")
        ruling("2015-08-25", "Vigilance matters only as attackers are declared. Giving a creature vigilance after that point won't untap it, even if it's attacking.")
    }
}
