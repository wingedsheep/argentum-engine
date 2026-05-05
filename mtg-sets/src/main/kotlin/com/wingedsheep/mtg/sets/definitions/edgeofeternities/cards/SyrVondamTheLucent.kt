package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Syr Vondam, the Lucent
 * {2}{W}{B}{B}
 * Legendary Creature — Human Knight
 * 4/4
 *
 * Deathtouch, lifelink
 * Whenever Syr Vondam enters or attacks, other creatures you control get +1/+0 and gain deathtouch until end of turn.
 */
val SyrVondamTheLucent = card("Syr Vondam, the Lucent") {
    manaCost = "{2}{W}{B}{B}"
    typeLine = "Legendary Creature — Human Knight"
    power = 4
    toughness = 4
    oracleText = "Deathtouch, lifelink\nWhenever Syr Vondam enters or attacks, other creatures you control get +1/+0 and gain deathtouch until end of turn."

    keywords(Keyword.DEATHTOUCH, Keyword.LIFELINK)

    // Whenever Syr Vondam enters the battlefield or attacks, other creatures you control get +1/+0 and gain deathtouch until end of turn
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = CompositeEffect(
            listOf(
                EffectPatterns.modifyStatsForAll(1, 0, GroupFilter.OtherCreaturesYouControl),
                EffectPatterns.grantKeywordToAll(Keyword.DEATHTOUCH, GroupFilter.OtherCreaturesYouControl)
            )
        )
        description = "other creatures you control get +1/+0 and gain deathtouch until end of turn"
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = CompositeEffect(
            listOf(
                EffectPatterns.modifyStatsForAll(1, 0, GroupFilter.OtherCreaturesYouControl),
                EffectPatterns.grantKeywordToAll(Keyword.DEATHTOUCH, GroupFilter.OtherCreaturesYouControl)
            )
        )
        description = "other creatures you control get +1/+0 and gain deathtouch until end of turn"
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "232"
        artist = "Cristi Balanescu"
        flavorText = "A new dawn, burning away the darkness."
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea954205-5ff5-493b-bf30-6212042c2bc9.jpg?1752947509"
    }
}
