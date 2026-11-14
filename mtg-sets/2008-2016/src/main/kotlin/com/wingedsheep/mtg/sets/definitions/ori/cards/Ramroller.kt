package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.MustAttack

/**
 * Ramroller — Magic Origins #237
 * {3} · Artifact Creature — Juggernaut · 2/3
 *
 * This creature attacks each combat if able.
 * This creature gets +2/+0 as long as you control another artifact.
 *
 * The attack requirement is the static [MustAttack]. The bonus is a [ConditionalStaticAbility]
 * over [Filters.Self] gated by [Conditions.YouControl] with `excludeSelf = true` — Ramroller is
 * itself an artifact, so "another" must not count it. The condition is re-read continuously in
 * Layer 7c, so the bonus comes and goes with the other artifact.
 */
val Ramroller = card("Ramroller") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Juggernaut"
    power = 2
    toughness = 3
    oracleText = "This creature attacks each combat if able.\n" +
        "This creature gets +2/+0 as long as you control another artifact."

    staticAbility {
        ability = MustAttack()
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(2, 0, Filters.Self),
            condition = Conditions.YouControl(GameObjectFilter.Artifact, excludeSelf = true),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "237"
        artist = "Craig J Spearing"
        flavorText = "As unstoppable as it is unsteerable."
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07f7ba4d-26bb-4631-a135-f27d94f376d1.jpg?1783938308"
        ruling("2015-06-22", "If, during its controller's declare attackers step, Ramroller is tapped or is affected by a spell or ability that says it can't attack, then Ramroller doesn't attack. If there's a cost associated with having it attack, its controller isn't forced to pay that cost. If they don't, Ramroller doesn't have to attack.")
    }
}
