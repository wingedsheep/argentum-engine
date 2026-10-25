package com.wingedsheep.mtg.sets.definitions.fic.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Y'shtola, Night's Blessed
 * {1}{W}{U}{B}
 * Legendary Creature — Cat Warlock
 * 2/4
 * Vigilance
 * At the beginning of each end step, if a player lost 4 or more life this turn, you draw a card.
 * Whenever you cast a noncreature spell with mana value 3 or greater, Y'shtola deals 2 damage to
 * each opponent and you gain 2 life.
 *
 * "A player lost 4 or more life" is a per-player threshold, so it is
 * `countPlayersWith(Each, lifeLostThisTurn() >= 4) >= 1` (Bloodchief Ascension's shape over every
 * player rather than only opponents) — an intervening "if", checked when the end step begins
 * (ruling).
 */
val YshtolaNightsBlessed = card("Y'shtola, Night's Blessed") {
    manaCost = "{1}{W}{U}{B}"
    colorIdentity = "WUB"
    typeLine = "Legendary Creature — Cat Warlock"
    power = 2
    toughness = 4
    oracleText = "Vigilance\n" +
        "At the beginning of each end step, if a player lost 4 or more life this turn, you draw a card.\n" +
        "Whenever you cast a noncreature spell with mana value 3 or greater, Y'shtola deals 2 damage to each opponent and you gain 2 life."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.countPlayersWith(
                scope = Player.Each,
                condition = Conditions.CompareAmounts(
                    DynamicAmounts.lifeLostThisTurn(),
                    ComparisonOperator.GTE,
                    4,
                ),
            ),
            ComparisonOperator.GTE,
            1,
        )
        effect = Effects.DrawCards(1)
        description = "At the beginning of each end step, if a player lost 4 or more life this turn, you draw a card."
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature.manaValueAtLeast(3))
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(2)
        description = "Whenever you cast a noncreature spell with mana value 3 or greater, Y'shtola deals 2 damage to each opponent and you gain 2 life."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "7"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7f2c2d5-e052-49e8-b5de-712858c2ea78.jpg?1783906373"
        ruling("2025-06-06", "For spells on the stack with {X} in their mana costs, use the value chosen for X to determine the spell's mana value.")
        ruling("2025-06-06", "Y'shtola's last ability resolves before the spell that caused it to trigger. It resolves even if that spell is countered or otherwise leaves the stack.")
        ruling("2025-06-06", "Y'shtola's second ability checks at the moment it would trigger to see if a player lost 4 or more life this turn. If none did, the ability won't trigger at all. Once an end step begins, it's too late to cause a player to lose life in order to cause this ability to trigger.")
    }
}
