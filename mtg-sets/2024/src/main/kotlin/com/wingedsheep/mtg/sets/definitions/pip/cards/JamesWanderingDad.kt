package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * James, Wandering Dad // Follow Him
 * {2}{U}
 * Legendary Creature — Human Scientist
 * 2/4
 *
 * {T}: Add {C}{C}. Spend this mana only to activate abilities.
 *
 * Adventure: Follow Him — {X}{U}{U}, Instant — Adventure
 * Investigate X times.
 *
 * The mana restriction is [ManaRestriction.AbilityActivationOnly] — any activated ability, of any
 * source (including the Clues Follow Him makes). Follow Him scales Investigate by the spell's X.
 */
val JamesWanderingDad = card("James, Wandering Dad") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Scientist"
    power = 2
    toughness = 4
    oracleText = "{T}: Add {C}{C}. Spend this mana only to activate abilities."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(2, restriction = ManaRestriction.AbilityActivationOnly)
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add {C}{C}. Spend this mana only to activate abilities."
    }

    adventure("Follow Him") {
        manaCost = "{X}{U}{U}"
        typeLine = "Instant — Adventure"
        oracleText = "Investigate X times. (Then exile this card. You may cast the creature later from exile.)"
        spell {
            effect = Effects.Investigate(DynamicAmounts.xValue())
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "31"
        artist = "Wonchun Choi"
        flavorText = "\"So long as we've got each other, that's all that matters.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31fed516-04e1-4453-b955-dfbd32bab2f3.jpg?1783912413"
    }
}
