package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mercurial Chemister (RTR #180) — {3}{U}{R} Creature — Human Wizard 2/3.
 *
 *   {U}, {T}: Draw two cards.
 *   {R}, {T}, Discard a card: This creature deals damage to target creature equal to the
 *   discarded card's mana value.
 *
 * The discard is part of the activation cost; the damage reads the paid card through
 * [EffectTarget.DiscardedAsCost] (an {X} card counts X as 0, a land as 0 — rulings).
 */
val MercurialChemister = card("Mercurial Chemister") {
    manaCost = "{3}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Human Wizard"
    oracleText = "{U}, {T}: Draw two cards.\n" +
        "{R}, {T}, Discard a card: This creature deals damage to target creature equal to the " +
        "discarded card's mana value."
    power = 2
    toughness = 3

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U}"), Costs.Tap)
        effect = Effects.DrawCards(2)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R}"), Costs.Tap, Costs.Discard())
        val victim = target(TargetFilter.Creature)
        effect = Effects.DealDamage(DynamicAmounts.manaValueOf(EffectTarget.DiscardedAsCost()), victim)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "180"
        artist = "Wesley Burt"
        flavorText = "What some see as \"distracted\" is really \"fathoming the unfathomable.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/8/881728ce-4b18-410e-9cdb-4d439ce0b21d.jpg?1783940336"
        ruling("2012-10-01", "If the discarded card has {X} in its mana cost, X is 0.")
        ruling(
            "2012-10-01",
            "If the discarded card has no mana symbols in its upper right corner (because it's a land " +
                "card, for example), its mana value is 0."
        )
    }
}
