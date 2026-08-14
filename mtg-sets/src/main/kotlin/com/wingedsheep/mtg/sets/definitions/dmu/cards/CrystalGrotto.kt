package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Crystal Grotto
 * Land
 *
 * When this land enters, scry 1.
 * {T}: Add {C}.
 * {1}, {T}: Add one mana of any color.
 *
 * Canonical definition lives here in Dominaria United, the card's earliest real printing; Wilds of
 * Eldraine contributes only a [com.wingedsheep.sdk.model.Printing] row
 * (`definitions/woe/cards/CrystalGrottoReprint.kt`).
 *
 * Both mana abilities are the same permanent's, and the {C} one is free — so the {1} filter ability
 * can be paid by first tapping the Grotto itself only if another source supplies the {1}; the
 * Grotto cannot fund its own filter, since {T} is part of both costs. Nothing special is needed to
 * model that: the shared {T} makes it fall out of ordinary cost payment.
 */
val CrystalGrotto = card("Crystal Grotto") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "When this land enters, scry 1.\n{T}: Add {C}.\n{1}, {T}: Add one mana of any color."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Library.scry(1)
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.AddManaOfChoice()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "246"
        artist = "Piotr Dura"
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd250c9d-c65f-4293-a6b0-007fac634d3d.jpg?1783921264"
    }
}
