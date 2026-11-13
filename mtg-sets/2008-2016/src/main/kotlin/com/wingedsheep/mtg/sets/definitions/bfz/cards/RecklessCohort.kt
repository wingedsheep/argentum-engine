package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MustAttack

/**
 * Reckless Cohort — Battle for Zendikar #152
 * {1}{R} · Creature — Human Warrior Ally · 2/2
 * This creature attacks each combat if able unless you control another Ally.
 *
 * Juggernaut's [MustAttack] gated by a [ConditionalStaticAbility] whose condition is "you control
 * no *other* Ally" — [Conditions.YouControl] with `negate = true` and `excludeSelf = true`, so the
 * Cohort (itself an Ally) never satisfies its own exemption. The gate is evaluated during
 * projection, so the requirement is read as attackers are declared (the printed ruling), and an
 * opponent's Ally doesn't count.
 */
val RecklessCohort = card("Reckless Cohort") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior Ally"
    power = 2
    toughness = 2
    oracleText = "This creature attacks each combat if able unless you control another Ally."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = MustAttack(),
            condition = Conditions.YouControl(
                GameObjectFilter.Permanent.withSubtype(Subtype.ALLY),
                negate = true,
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "152"
        artist = "Volkan Baǵa"
        flavorText = "\"You have a family. Mine died at Sea Gate. You go to yours, and I'll go to mine.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/7/77a4cdec-e5fc-4ea8-a7bc-f1109fadae3c.jpg?1783938192"
        ruling(
            "2015-08-25",
            "Reckless Cohort can attack if you control another Ally. It just isn't forced to."
        )
        ruling(
            "2015-08-25",
            "Reckless Cohort checks whether you control another Ally as attacking creatures are " +
                "declared. At that time, if you don't, Reckless Cohort attacks if able."
        )
    }
}
