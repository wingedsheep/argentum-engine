package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GainKeywordsOfCards
import com.wingedsheep.sdk.scripting.KeywordCardPool
import com.wingedsheep.sdk.scripting.KeywordFamily
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Urborg Scavengers — March of the Machine: The Aftermath #15
 * {2}{B} · Creature — Spirit · 2/2
 *
 * "Enters or attacks" is authored as two triggered abilities with the same body. Each exiles its
 * target into this creature's linked-exile pile (CR 607), and [GainKeywordsOfCards] over
 * [KeywordCardPool.CARDS_EXILED_WITH_SOURCE] reads only that pile. "Hexproof" is a family, so a
 * card with hexproof from a quality grants that hexproof-from ability. Protection is not on the list.
 */
val UrborgScavengers = card("Urborg Scavengers") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature enters or attacks, exile target card from a graveyard. " +
        "Put a +1/+1 counter on this creature.\n" +
        "This creature has flying as long as a card exiled with it has flying. The same is true for " +
        "first strike, double strike, deathtouch, haste, hexproof, indestructible, lifelink, menace, " +
        "reach, trample, and vigilance."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.CardInGraveyard)
        effect = Effects.ExileLinkedToSource(t) then
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever this creature enters, exile target card from a graveyard. " +
            "Put a +1/+1 counter on this creature."
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val t = target(TargetFilter.CardInGraveyard)
        effect = Effects.ExileLinkedToSource(t) then
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever this creature attacks, exile target card from a graveyard. " +
            "Put a +1/+1 counter on this creature."
    }

    staticAbility {
        ability = GainKeywordsOfCards(
            keywords = listOf(
                Keyword.FLYING,
                Keyword.FIRST_STRIKE,
                Keyword.DOUBLE_STRIKE,
                Keyword.DEATHTOUCH,
                Keyword.HASTE,
                Keyword.INDESTRUCTIBLE,
                Keyword.LIFELINK,
                Keyword.MENACE,
                Keyword.REACH,
                Keyword.TRAMPLE,
                Keyword.VIGILANCE
            ),
            families = setOf(KeywordFamily.HEXPROOF),
            cards = KeywordCardPool.CARDS_EXILED_WITH_SOURCE
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "15"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4dbb4d8f-8ae3-4052-9c6a-e56e96cec832.jpg?1783916524"
        ruling("2023-05-12", "If the target of the first ability is an illegal target at the time the ability tries to resolve (likely because it left the graveyard in response), the ability won't resolve and none of its effects will happen. You won't put a +1/+1 counter on Urborg Scavengers.")
    }
}
