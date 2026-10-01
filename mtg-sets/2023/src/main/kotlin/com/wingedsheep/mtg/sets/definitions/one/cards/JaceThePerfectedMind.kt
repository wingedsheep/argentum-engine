package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Jace, the Perfected Mind — Phyrexia: All Will Be One #57
 * {2}{U}{U/P} · Legendary Planeswalker — Jace · Starting loyalty 5
 *
 * Compleated
 * +1: Until your next turn, up to one target creature gets -3/-0.
 * −2: Target player mills three cards. Then if a graveyard has twenty or more cards in it, you
 *     draw three cards. Otherwise, you draw a card.
 * −X: Target player mills three times X cards.
 *
 * "A graveyard" is existential over every player's graveyard (per the ruling, not just the
 * target's): `countPlayersWith(Player.Each, CardsInGraveyardAtLeast(20)) >= 1`, where
 * `countPlayersWith` rebinds `Player.You` to each candidate player in turn. The check runs after
 * the mill, so the three milled cards count.
 */
val JaceThePerfectedMind = card("Jace, the Perfected Mind") {
    manaCost = "{2}{U}{U/P}"
    colorIdentity = "U"
    typeLine = "Legendary Planeswalker — Jace"
    startingLoyalty = 5
    oracleText = "Compleated ({U/P} can be paid with {U} or 2 life. If life was paid, this planeswalker enters with two fewer loyalty counters.)\n" +
        "+1: Until your next turn, up to one target creature gets -3/-0.\n" +
        "−2: Target player mills three cards. Then if a graveyard has twenty or more cards in it, you draw three cards. Otherwise, you draw a card.\n" +
        "−X: Target player mills three times X cards."

    keywords(Keyword.COMPLEATED)

    loyaltyAbility(+1) {
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.ModifyStats(-3, 0, creature, Duration.UntilYourNextTurn)
    }

    loyaltyAbility(-2) {
        val player = target(Targets.Player)
        val aGraveyardHasTwenty = Conditions.CompareAmounts(
            DynamicAmounts.countPlayersWith(Player.Each, Conditions.CardsInGraveyardAtLeast(20)),
            ComparisonOperator.GTE,
            1
        )
        effect = Patterns.Library.mill(3, player) then
            Effects.If(
                aGraveyardHasTwenty,
                then = Effects.DrawCards(3),
                otherwise = Effects.DrawCards(1)
            )
    }

    loyaltyAbilityX {
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(DynamicAmounts.xValue() * 3, player)
        description = "Target player mills three times X cards."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "57"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64e6a8d1-ae75-45bd-af62-9a622620cb5c.jpg?1783918061"
        ruling("2023-02-04", "Jace's second loyalty ability doesn't care which player's graveyard has twenty or more cards in it. It could be the target player's or another player's.")
        ruling("2023-02-04", "A Phyrexian mana symbol contributes 1 toward the mana value of a card, even if life is paid for it. Specifically, Jace's mana value is always 4.")
        ruling("2023-02-04", "The compleated ability looks only at whether a player chose to pay 2 life for a Phyrexian mana symbol as they were casting the spell. If a player paid life for some other reason while casting the spell, that will not reduce the number of loyalty counters the planeswalker enters the battlefield with.")
        ruling("2023-02-04", "Other replacement effects that would change the number of loyalty counters Jace enters with will apply as normal.")
    }
}
