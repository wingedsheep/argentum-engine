package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Chainweb Aracnir
 * {G}
 * Creature — Spider
 * 1/2
 * Reach
 * When this creature enters, it deals damage equal to its power to target creature with flying an
 * opponent controls.
 * Escape—{3}{G}{G}, Exile four other cards from your graveyard.
 * This creature escapes with three +1/+1 counters on it.
 *
 * The damage reads [DynamicAmounts.sourcePower] at resolution (last-known power if the Aracnir has
 * left, per the ruling); escaped, it enters with its three counters first, so the trigger deals 4.
 * Escape and "escapes with" are Woe Strider's shape: [KeywordAbility.escape] plus an
 * [EntersWithCounters] replacement gated on [Conditions.Escaped].
 */
val ChainwebAracnir = card("Chainweb Aracnir") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spider"
    power = 1
    toughness = 2
    oracleText = "Reach\n" +
        "When this creature enters, it deals damage equal to its power to target creature with flying an " +
        "opponent controls.\n" +
        "Escape—{3}{G}{G}, Exile four other cards from your graveyard. " +
        "(You may cast this card from your graveyard for its escape cost.)\n" +
        "This creature escapes with three +1/+1 counters on it."

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val flier = target(TargetFilter.Creature.withKeyword(Keyword.FLYING).opponentControls())
        effect = Effects.DealDamage(DynamicAmounts.sourcePower(), flier)
        description = "When this creature enters, it deals damage equal to its power to target creature " +
            "with flying an opponent controls."
    }

    keywordAbility(KeywordAbility.escape("{3}{G}{G}", Costs.additional.ExileOtherCards(4)))

    replacementEffect(EntersWithCounters(
        counterType = CounterType.PLUS_ONE_PLUS_ONE,
        count = 3,
        selfOnly = true,
        condition = Conditions.Escaped
    ))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "167"
        artist = "Sam Rowan"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/874afaad-546a-4cb2-ad01-2b4a4862b219.jpg?1783931542"
        ruling(
            "2020-01-24",
            "If Chainweb Aracnir leaves the battlefield before its triggered ability resolves, use its power " +
                "as it last existed on the battlefield to determine how much damage it deals."
        )
        ruling(
            "2020-01-24",
            "After an escaped spell resolves, it returns to its owner's graveyard if it's not a permanent " +
                "spell. If it is a permanent spell, it enters the battlefield and will return to its owner's " +
                "graveyard if it dies later. Perhaps it will escape again—good underworld security is so hard " +
                "to come by these days."
        )
        ruling(
            "2020-01-24",
            "If you cast a spell with its escape permission, you can't choose to apply any other alternative " +
                "costs or to cast it without paying its mana cost. If it has any additional costs, you must pay those."
        )
    }
}
