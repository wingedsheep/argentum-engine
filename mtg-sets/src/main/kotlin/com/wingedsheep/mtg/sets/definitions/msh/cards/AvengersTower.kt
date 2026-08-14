package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Avengers Tower
 * Land
 * {T}: Add {C}.
 * {T}: Add one mana of any color. Spend this mana only to cast a Hero spell or to activate an
 * ability of a Hero source.
 * {4}, {T}: Look at the top three cards of your library. You may reveal a Hero card from among
 * them and put it into your hand. Put the rest on the bottom of your library in any order.
 *
 * Implementation note: the filtered mana is the Unclaimed Territory shape —
 * [ManaRestriction.SubtypeSpellsOrAbilitiesOnly] with `creatureOnly = false`, which is exactly
 * "cast a Hero spell **or** activate an ability of a Hero source". The dig composes the stock
 * [Patterns.Library.lookAtTopRevealMatchingToHand] recipe; "in any order" makes the remainder
 * [CardOrder.ControllerChooses] rather than the more common random bottoming.
 */
val AvengersTower = card("Avengers Tower") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{T}: Add one mana of any color. Spend this mana only to cast a Hero spell or to " +
        "activate an ability of a Hero source.\n" +
        "{4}, {T}: Look at the top three cards of your library. You may reveal a Hero card from " +
        "among them and put it into your hand. Put the rest on the bottom of your library in any " +
        "order."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(
            amount = 1,
            restriction = ManaRestriction.SubtypeSpellsOrAbilitiesOnly("Hero", creatureOnly = false)
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = DynamicAmount.Fixed(3),
            filter = GameObjectFilter.Any.withSubtype(Subtype.HERO),
            prompt = "You may reveal a Hero card and put it into your hand",
            restOrder = CardOrder.ControllerChooses,
        )
        description = "Look at the top three cards of your library. You may reveal a Hero card " +
            "from among them and put it into your hand. Put the rest on the bottom of your " +
            "library in any order."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "260"
        artist = "Arthur Yuan"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88f0d9c9-8a1f-4b5a-b6f9-821ddd658d27.jpg?1783902887"
    }
}
