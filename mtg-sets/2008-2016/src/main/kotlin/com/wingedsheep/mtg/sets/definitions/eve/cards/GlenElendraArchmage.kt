package com.wingedsheep.mtg.sets.definitions.eve.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Glen Elendra Archmage — Eventide #22 (canonical / earliest real-expansion printing).
 * {3}{U} · Creature — Faerie Wizard · 2/2
 *
 * Flying
 * {U}, Sacrifice this creature: Counter target noncreature spell.
 * Persist
 *
 * Persist is engine-live through [Keyword.PERSIST] alone (cf. Safehold Elite); the counter is the
 * standard noncreature-spell counter (Stubborn Denial) behind a {U} + sacrifice-self cost.
 */
val GlenElendraArchmage = card("Glen Elendra Archmage") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Faerie Wizard"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "{U}, Sacrifice this creature: Counter target noncreature spell.\n" +
        "Persist (When this creature dies, if it had no -1/-1 counters on it, return it to the " +
        "battlefield under its owner's control with a -1/-1 counter on it.)"

    keywords(Keyword.FLYING, Keyword.PERSIST)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U}"), Costs.SacrificeSelf)
        target(TargetFilter.NoncreatureSpellOnStack)
        effect = Effects.CounterSpell()
        description = "{U}, Sacrifice this creature: Counter target noncreature spell."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "22"
        artist = "Warren Mahy"
        imageUri = "https://cards.scryfall.io/normal/front/0/9/09516d3d-e6c2-4359-af2a-a4aa244ca033.jpg?1783942690"
        ruling(
            "2013-06-07",
            "The persist ability triggers when the permanent is put into a graveyard. Its last known " +
                "information (that is, how the creature last existed on the battlefield) is used to " +
                "determine whether it had a -1/-1 counter on it."
        )
        ruling(
            "2013-06-07",
            "When a permanent with persist returns to the battlefield, it's a new object with no memory " +
                "of or connection to its previous existence."
        )
    }
}
