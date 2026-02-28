package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaEffect
import com.wingedsheep.sdk.scripting.effects.GrantActivatedAbilityToGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Divergent Growth
 * {G}
 * Instant
 * Until end of turn, lands you control gain "{T}: Add one mana of any color."
 */
val DivergentGrowth = card("Divergent Growth") {
    manaCost = "{G}"
    typeLine = "Instant"
    oracleText = "Until end of turn, lands you control gain \"{T}: Add one mana of any color.\""

    spell {
        effect = GrantActivatedAbilityToGroupEffect(
            ability = ActivatedAbility(
                id = AbilityId.generate(),
                cost = AbilityCost.Tap,
                effect = AddAnyColorManaEffect(1),
                isManaAbility = true,
                timing = TimingRule.ManaAbility
            ),
            filter = GroupFilter(GameObjectFilter.Companion.Land.youControl()),
            duration = Duration.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "116"
        artist = "Rob Alexander"
        flavorText = "Nature has forgotten its own rules."
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e609448-7868-4e28-b399-3750556a693c.jpg?1562525426"
    }
}
