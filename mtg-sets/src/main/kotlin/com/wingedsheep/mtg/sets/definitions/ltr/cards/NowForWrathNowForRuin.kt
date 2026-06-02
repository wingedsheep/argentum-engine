package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Now for Wrath, Now for Ruin!
 * {3}{W}
 * Sorcery
 *
 * Put a +1/+1 counter on each creature you control. They gain vigilance until end of turn.
 * The Ring tempts you.
 */
val NowForWrathNowForRuin = card("Now for Wrath, Now for Ruin!") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Put a +1/+1 counter on each creature you control. They gain vigilance until end of turn. The Ring tempts you."

    spell {
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesYouControl,
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
                .then(GrantKeywordEffect(Keyword.VIGILANCE.name, EffectTarget.Self, Duration.EndOfTurn))
        ).then(Effects.TheRingTemptsYou())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "24"
        artist = "Valera Lutfullina"
        flavorText = "The Captains of the West came at last to challenge the Black Gate and the might of Mordor."
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2e2c80d-7581-46ba-8237-7886b91c19b3.jpg?1687694562"
    }
}
