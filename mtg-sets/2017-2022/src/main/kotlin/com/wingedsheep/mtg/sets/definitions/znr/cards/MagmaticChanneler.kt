package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Magmatic Channeler
 * {1}{R}
 * Creature — Human Wizard
 * 1/3
 * As long as there are four or more instant and/or sorcery cards in your graveyard, this
 * creature gets +3/+1.
 * {T}, Discard a card: Exile the top two cards of your library, then choose one of them. You may
 * play that card this turn.
 *
 * The static is Ghitu Lavarunner's graveyard-count condition; the activated ability is
 * Party Thrasher's exile-two-choose-one pipeline behind Hollowhead Sliver's tap-and-discard cost.
 */
val MagmaticChanneler = card("Magmatic Channeler") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 3
    oracleText = "As long as there are four or more instant and/or sorcery cards in your graveyard, " +
        "this creature gets +3/+1.\n" +
        "{T}, Discard a card: Exile the top two cards of your library, then choose one of them. " +
        "You may play that card this turn."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantDynamicStats(
                filter = GroupFilter.source(),
                powerBonus = DynamicAmounts.fixed(3),
                toughnessBonus = DynamicAmounts.fixed(1)
            ),
            condition = Conditions.CompareAmounts(
                DynamicAmounts.count(Player.You, Zone.GRAVEYARD, GameObjectFilter.InstantOrSorcery),
                ComparisonOperator.GTE,
                4
            )
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.DiscardCard)
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(2))
            exile(exiled)
            val chosen = chooseExactly(1, from = exiled, prompt = "Choose a card you may play this turn")
            run(Effects.GrantMayPlayFromExile(from = chosen))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "148"
        artist = "Bryan Sola"
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d8be6570-5b5f-4ffc-a5e5-67d7c41c8caa.jpg?1783929356"
        ruling("2020-09-25", "Magmatic Channeler gets just +3/+1 from its first ability, no matter how many instant and sorcery cards are in your graveyard beyond the fourth.")
        ruling("2020-09-25", "You choose which card you'll be allowed to play this turn while Magmatic Channeler's ability is resolving, but you must follow the normal timing permissions and restrictions for the card when you play it. If it's a land, you can't play it unless you have land plays available.")
        ruling("2020-09-25", "If you don't play the exiled card you chose, it remains exiled. The card you didn't choose remains exiled whether you play the chosen card or not.")
    }
}
