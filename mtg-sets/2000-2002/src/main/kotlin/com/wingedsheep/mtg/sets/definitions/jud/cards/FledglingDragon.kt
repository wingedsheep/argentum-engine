package com.wingedsheep.mtg.sets.definitions.jud.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fledgling Dragon — Judgment #90
 * {2}{R}{R} · Creature — Dragon · 2 / 2
 *
 * Flying
 * Threshold — As long as there are seven or more cards in your graveyard, this creature gets
 * +3/+3 and has "{R}: This creature gets +1/+0 until end of turn."
 *
 * Threshold is an ability word: both halves are self-scoped statics behind the same
 * [Conditions.CardsInGraveyardAtLeast] gate. The firebreathing ability is *granted*, not printed
 * with an activation restriction, so it only exists while threshold holds; a pump already
 * resolved stays for the turn even if the graveyard later drops below seven.
 */
val FledglingDragon = card("Fledgling Dragon") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "Threshold — As long as there are seven or more cards in your graveyard, this creature gets +3/+3 " +
        "and has \"{R}: This creature gets +1/+0 until end of turn.\""

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(powerBonus = 3, toughnessBonus = 3, filter = GroupFilter.source()),
            condition = Conditions.CardsInGraveyardAtLeast(7),
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantActivatedAbility(
                ability = grantedActivatedAbility {
                    cost = Costs.Mana("{R}")
                    effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
                    description = "{R}: This creature gets +1/+0 until end of turn."
                },
                filter = GroupFilter.source(),
            ),
            condition = Conditions.CardsInGraveyardAtLeast(7),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "90"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/315e5b4e-ae58-412a-be27-c4ef4899fbbd.jpg?1783945118"
    }
}
