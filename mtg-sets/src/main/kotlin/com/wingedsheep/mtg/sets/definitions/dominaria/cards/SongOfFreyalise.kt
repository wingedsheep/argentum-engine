package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaEffect
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.effects.GrantActivatedAbilityToGroupEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Song of Freyalise
 * {1}{G}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I, II — Until your next turn, creatures you control gain "{T}: Add one mana of any color."
 * III — Put a +1/+1 counter on each creature you control. Those creatures gain vigilance,
 *        trample, and indestructible until end of turn.
 */
val SongOfFreyalise = card("Song of Freyalise") {
    manaCost = "{1}{G}"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I, II — Until your next turn, creatures you control gain \"{T}: Add one mana of any color.\"\n" +
        "III — Put a +1/+1 counter on each creature you control. Those creatures gain vigilance, " +
        "trample, and indestructible until end of turn."

    val creaturesYouControl = GroupFilter.AllCreaturesYouControl

    val manaAbility = GrantActivatedAbilityToGroupEffect(
        ability = ActivatedAbility(
            id = AbilityId.generate(),
            cost = AbilityCost.Tap,
            effect = AddAnyColorManaEffect(1),
            isManaAbility = true,
            timing = TimingRule.ManaAbility
        ),
        filter = creaturesYouControl,
        duration = Duration.UntilYourNextTurn
    )

    sagaChapter(1) {
        effect = manaAbility
    }

    sagaChapter(2) {
        effect = manaAbility
    }

    sagaChapter(3) {
        effect = CompositeEffect(listOf(
            ForEachInGroupEffect(
                filter = creaturesYouControl,
                effect = AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
            ),
            ForEachInGroupEffect(
                filter = creaturesYouControl,
                effect = CompositeEffect(listOf(
                    GrantKeywordEffect(Keyword.VIGILANCE.name, EffectTarget.Self, Duration.EndOfTurn),
                    GrantKeywordEffect(Keyword.TRAMPLE.name, EffectTarget.Self, Duration.EndOfTurn),
                    GrantKeywordEffect(Keyword.INDESTRUCTIBLE.name, EffectTarget.Self, Duration.EndOfTurn)
                ))
            )
        ))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "179"
        artist = "Min Yum"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a45b534b-8442-4074-a8d2-f38e83f24868.jpg?1562740656"
        ruling("2018-04-27", "Each of Song of Freyalise's chapter abilities affects only creatures you control at the time it resolves.")
    }
}
