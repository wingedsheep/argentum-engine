package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Loot, the Pathfinder
 * {2}{G}{U}{R}
 * Legendary Creature — Beast Noble
 * 2/4
 * Double strike, vigilance, haste
 * Exhaust — {G}, {T}: Add three mana of any one color.
 * Exhaust — {U}, {T}: Draw three cards.
 * Exhaust — {R}, {T}: Loot deals 3 damage to any target.
 *
 * Each exhaust ability is once per object (`isExhaust`); all three share the {T} symbol, so in
 * practice only one fires per untap step unless Loot is untapped again.
 */
val LootThePathfinder = card("Loot, the Pathfinder") {
    manaCost = "{2}{G}{U}{R}"
    colorIdentity = "GRU"
    typeLine = "Legendary Creature — Beast Noble"
    oracleText = "Double strike, vigilance, haste\n" +
        "Exhaust — {G}, {T}: Add three mana of any one color. (Activate each exhaust ability only once.)\n" +
        "Exhaust — {U}, {T}: Draw three cards.\n" +
        "Exhaust — {R}, {T}: Loot deals 3 damage to any target."
    power = 2
    toughness = 4
    keywords(Keyword.DOUBLE_STRIKE, Keyword.VIGILANCE, Keyword.HASTE)
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.Tap)
        isExhaust = true
        effect = Effects.AddAnyColorMana(3)
        // Adds mana, targets nothing — a mana ability (CR 605.1a), so it uses the stack-free
        // timing rule. Pit Automaton's "exhaust ability that isn't a mana ability" rider only
        // makes sense if this one is flagged as one.
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U}"), Costs.Tap)
        isExhaust = true
        effect = Effects.DrawCards(3)
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R}"), Costs.Tap)
        isExhaust = true
        val t = target("target", AnyTarget())
        effect = Effects.DealDamage(3, t)
    }
    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "212"
        artist = "Ernanda Souza"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33c59c04-4c0b-4a60-826e-3a7757d0b2a2.jpg?1783907856"
        ruling("2025-02-07", "Exhaust abilities can be activated any time you could normally activate an ability.")
        ruling(
            "2025-02-07",
            "If an exhaust ability of a permanent is activated, and then that permanent leaves the " +
                "battlefield and returns to the battlefield, it becomes a new object so its exhaust " +
                "ability can be activated again."
        )
    }
}
