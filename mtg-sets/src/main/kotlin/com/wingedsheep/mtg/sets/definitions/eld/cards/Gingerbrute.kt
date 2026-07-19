package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gingerbrute
 * {1}
 * Artifact Creature — Food Golem
 * 1/1
 *
 * Haste
 * {1}: This creature can't be blocked this turn except by creatures with haste.
 * {2}, {T}, Sacrifice this creature: You gain 3 life.
 *
 * Canonical earliest real printing is Throne of Eldraine (ELD, 2019); reprinted in Wilds of
 * Eldraine (WOE) — see GingerbruteReprint in the WOE package.
 *
 * The "Food" subtype on the type line makes it a Food artifact for cards that care, in addition
 * to carrying its own sacrifice-for-life ability (identical to a Food token's).
 */
val Gingerbrute = card("Gingerbrute") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Food Golem"
    oracleText = "Haste (This creature can attack and {T} as soon as it comes under your control.)\n" +
        "{1}: This creature can't be blocked this turn except by creatures with haste.\n" +
        "{2}, {T}, Sacrifice this creature: You gain 3 life."
    power = 1
    toughness = 1

    keywords(Keyword.HASTE)

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = Effects.GrantCantBeBlockedExceptBy(
            EffectTarget.Self,
            GameObjectFilter.Creature.withKeyword(Keyword.HASTE)
        )
        description = "This creature can't be blocked this turn except by creatures with haste."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.Tap,
            Costs.SacrificeSelf
        )
        effect = Effects.GainLife(3)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "219"
        artist = "Vincent Proce"
        flavorText = "The unlabeled vial was not vanilla extract after all."
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f55fe038-c903-4d92-b689-72dd6d041a91.jpg?1783932587"
    }
}
