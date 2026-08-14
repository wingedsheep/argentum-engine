package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Master's Councillors — The Hobbit #47
 * {1}{U} · Creature — Human Advisor · Uncommon
 * 1/3
 *
 * Vigilance
 * This creature gets +2/+0 for each graveyard with seven or more cards in it.
 * Whenever you draw your second card each turn, target player mills three cards.
 *
 * Modeling notes:
 *  - "Each graveyard" is every player's graveyard, the controller's included, so the scope of
 *    [DynamicAmount.CountPlayersWith] is [Player.Each]. That primitive rebinds `Player.You` inside
 *    its condition to each candidate player in turn, which makes
 *    [Conditions.CardsInGraveyardAtLeast] the per-graveyard test. The count doubles into a power
 *    bonus via [DynamicAmount.Multiply]; toughness is untouched.
 *  - The pump is a static ability rather than a printed `*` power, so it stacks with counters and
 *    other effects in the normal layer order and recomputes as graveyards grow and shrink.
 *  - [Triggers.NthCardDrawn] fires exactly once per turn, on the draw that crosses the second
 *    card — including a single draw-two that crosses it in one event.
 */
val MastersCouncillors = card("Master's Councillors") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Advisor"
    power = 1
    toughness = 3
    oracleText = "Vigilance\n" +
        "This creature gets +2/+0 for each graveyard with seven or more cards in it.\n" +
        "Whenever you draw your second card each turn, target player mills three cards. " +
        "(They put the top three cards of their library into their graveyard.)"

    keywords(Keyword.VIGILANCE)

    staticAbility {
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmount.Multiply(
                DynamicAmount.CountPlayersWith(
                    scope = Player.Each,
                    condition = Conditions.CardsInGraveyardAtLeast(7)
                ),
                2
            ),
            toughnessBonus = DynamicAmount.Fixed(0)
        )
    }

    triggeredAbility {
        trigger = Triggers.NthCardDrawn(2)
        val milled = target("player", TargetPlayer())
        effect = Patterns.Library.mill(count = 3, target = milled)
        description = "Whenever you draw your second card each turn, target player mills three cards."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "47"
        artist = "Narendra Bintara Adi"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/addcefdd-e012-4adf-9052-e60376a8d2d3.jpg?1784798124"
    }
}
