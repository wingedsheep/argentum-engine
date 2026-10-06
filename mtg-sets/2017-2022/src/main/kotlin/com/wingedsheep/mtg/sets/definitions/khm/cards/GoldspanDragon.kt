package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Goldspan Dragon
 * {3}{R}{R}
 * Creature — Dragon
 * 4/4
 * Flying, haste
 * Whenever this creature attacks or becomes the target of a spell, create a Treasure token.
 * Treasures you control have "{T}, Sacrifice this artifact: Add two mana of any one color."
 *
 * The granted ability is an *additional* mana ability: each Treasure keeps its own
 * "{T}, Sacrifice: Add one mana of any color" alongside it.
 */
val GoldspanDragon = card("Goldspan Dragon") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying, haste\n" +
        "Whenever this creature attacks or becomes the target of a spell, create a Treasure token.\n" +
        "Treasures you control have \"{T}, Sacrifice this artifact: Add two mana of any one color.\""

    keywords(Keyword.FLYING, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.or(
            Triggers.self.attacks(),
            Triggers.self.becomesTarget(spellsOnly = true),
        )
        effect = Effects.CreateTreasure(1)
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf),
                effect = Effects.AddAnyColorMana(2),
                isManaAbility = true,
                timing = TimingRule.ManaAbility,
            ),
            filter = GroupFilter(GameObjectFilter.Artifact.withSubtype("Treasure").youControl()),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "139"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d914868-9000-4df2-a818-0ef8a7f636ae.jpg?1783928228"
        ruling("2021-02-05", "If a spell targets Goldspan Dragon more than once, the triggered ability will trigger only once.")
        ruling("2021-02-05", "An ability that triggers when a creature becomes the target of a spell resolves before the spell that caused it to trigger. Such an ability resolves even if that spell is countered.")
    }
}
