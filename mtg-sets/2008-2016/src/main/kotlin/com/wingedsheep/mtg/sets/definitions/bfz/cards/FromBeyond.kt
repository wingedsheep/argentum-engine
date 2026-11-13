package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * From Beyond
 * {3}{G}
 * Enchantment
 * Devoid (This card has no color.)
 * At the beginning of your upkeep, create a 1/1 colorless Eldrazi Scion creature token.
 * It has "Sacrifice this token: Add {C}."
 * {1}{G}, Sacrifice this enchantment: Search your library for an Eldrazi card, reveal it,
 * put it into your hand, then shuffle.
 *
 * The Scion token is the same inline shape as Call the Scions'. "An Eldrazi card" means the
 * Eldrazi subtype, not devoid or the name (ruling), so the search filters on the subtype alone.
 */
val FromBeyond = card("From Beyond") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Devoid (This card has no color.)\n" +
        "At the beginning of your upkeep, create a 1/1 colorless Eldrazi Scion creature token. " +
        "It has \"Sacrifice this token: Add {C}.\"\n" +
        "{1}{G}, Sacrifice this enchantment: Search your library for an Eldrazi card, reveal it, " +
        "put it into your hand, then shuffle."

    keywords(Keyword.DEVOID)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Eldrazi", "Scion"),
            activatedAbilities = listOf(
                ActivatedAbility(
                    cost = Costs.SacrificeSelf,
                    effect = Effects.AddColorlessMana(1),
                    isManaAbility = true,
                    timing = TimingRule.ManaAbility,
                    descriptionOverride = "Sacrifice this token: Add {C}.",
                )
            ),
            imageUri = "https://cards.scryfall.io/normal/front/b/9/b999a0fe-d2d0-4367-9abb-6ce5f3764f19.jpg?1783938121",
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{G}"), Costs.SacrificeSelf)
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Any.withSubtype("Eldrazi"),
            destination = SearchDestination.HAND,
            reveal = true,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "167"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5559960-e306-4992-b607-9654327c4507.jpg?1783938189"
        ruling(
            "2015-08-25",
            "An Eldrazi card is one with the creature type Eldrazi. Just having \"Eldrazi\" in its name " +
                "or being an Eldrazi-themed card doesn't count."
        )
        ruling(
            "2015-08-25",
            "Sacrificing an Eldrazi Scion creature token to add {C} is a mana ability. " +
                "It doesn't use the stack and can't be responded to."
        )
    }
}
