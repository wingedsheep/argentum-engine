package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Archway of Innovation — Modern Horizons 3 #214
 * Land · Rare
 *
 * This land enters tapped unless you control an Island.
 * {T}: Add {U}.
 * {U}, {T}: The next spell you cast this turn has improvise.
 *
 * The improvise ability installs a one-shot [Effects.GrantNextSpellKeyword] rider: the next spell
 * its controller casts this turn — from any zone — has improvise, and that cast spends the rider.
 */
val ArchwayOfInnovation = card("Archway of Innovation") {
    typeLine = "Land"
    colorIdentity = "U"
    oracleText = "This land enters tapped unless you control an Island.\n" +
        "{T}: Add {U}.\n" +
        "{U}, {T}: The next spell you cast this turn has improvise. (Your artifacts can help cast " +
        "that spell. Each artifact you tap after you're done activating mana abilities pays for {1}.)"

    replacementEffect(EntersTapped(
        unlessCondition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Island"))
    ))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Composite(
            listOf(
                Costs.Mana(ManaCost.parse("{U}")),
                AbilityCost.Tap
            )
        )
        effect = Effects.GrantNextSpellKeyword(Keyword.IMPROVISE)
        description = "The next spell you cast this turn has improvise."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "214"
        artist = "Yeong-Hao Han"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a90f9e6-9251-4203-9599-cc4032a5e6e1.jpg"
    }
}
