package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cait, Cage Brawler — indestructible gated on [Conditions.IsYourTurn].
 *
 * The attack trigger: you and the defending player each draw a card, then each chooses a card
 * from their own hand (you first — it's your turn, so APNAP), and both chosen cards are discarded.
 * "The greatest mana value among those cards or tied" is your discarded card's mana value being at
 * least the defending player's. A player with an empty hand discards nothing; an empty pick reads
 * as mana value 0, so a defending player who discarded nothing never beats you. The ruling's
 * other side — if *you* discarded nothing, Cait gets no counters whatever the defending player
 * discarded — is the separate "you discarded a card" half of the gate.
 */
val CaitCageBrawler = card("Cait, Cage Brawler") {
    manaCost = "{R}{G}"
    typeLine = "Legendary Creature — Human Warrior"
    power = 1
    toughness = 1
    oracleText = "During your turn, Cait has indestructible.\n" +
        "Whenever Cait attacks, you and defending player each draw a card, then discard a card. " +
        "Put two +1/+1 counters on Cait if you discarded the card with the greatest mana value " +
        "among those cards or tied for greatest."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.INDESTRUCTIBLE, Filters.Self),
            condition = Conditions.IsYourTurn,
        )
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.DrawCards(1) then
            Effects.DrawCards(1, EffectTarget.PlayerRef(Player.DefendingPlayer)) then
            Effects.Pipeline {
                val yourHand = gather(CardSource.FromZone(Zone.HAND, Player.You))
                val yours = chooseExactly(1, from = yourHand, prompt = "Choose a card to discard")
                val theirHand = gather(CardSource.FromZone(Zone.HAND, Player.DefendingPlayer))
                val theirs = chooseExactly(
                    1,
                    from = theirHand,
                    chooser = Chooser.DefendingPlayer,
                    prompt = "Choose a card to discard",
                )
                discard(yours)
                discard(theirs, Player.DefendingPlayer)
                run(
                    Effects.If(
                        condition = Conditions.All(
                            whenMatches(yours),
                            Conditions.CompareAmounts(
                                DynamicAmounts.manaValueOf(yours),
                                ComparisonOperator.GTE,
                                DynamicAmounts.manaValueOf(theirs),
                            ),
                        ),
                        then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self),
                    )
                )
            }
        description = "Whenever Cait attacks, you and defending player each draw a card, then discard " +
            "a card. Put two +1/+1 counters on Cait if you discarded the card with the greatest mana " +
            "value among those cards or tied for greatest."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "96"
        artist = "Joseph Meehan"
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fca61d02-2819-483f-9d30-862f0650ab0a.jpg?1783912393"
        ruling("2024-03-08", "If a card you discard has {X} in its mana cost, X is considered to be 0.")
        ruling("2024-03-08", "If you somehow can't discard a card (probably because you had no cards in hand and another effect prevented you from drawing cards), you won't put any counters on Cait no matter whether or not the defending player discarded a card or what that card's mana value is.")
    }
}
