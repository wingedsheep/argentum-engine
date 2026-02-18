package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.GroupFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MayEffect
import com.wingedsheep.sdk.scripting.OnCycle

/**
 * Dirge of Dread
 * {2}{B}
 * Sorcery
 * All creatures gain fear until end of turn.
 * Cycling {1}{B}
 * When you cycle Dirge of Dread, you may have target creature gain fear until end of turn.
 */
val DirgeOfDread = card("Dirge of Dread") {
    manaCost = "{2}{B}"
    typeLine = "Sorcery"
    oracleText = "All creatures gain fear until end of turn.\nCycling {1}{B}\nWhen you cycle Dirge of Dread, you may have target creature gain fear until end of turn."

    spell {
        effect = ForEachInGroupEffect(
            filter = GroupFilter.AllCreatures,
            effect = GrantKeywordUntilEndOfTurnEffect(Keyword.FEAR, EffectTarget.Self)
        )
    }

    keywordAbility(KeywordAbility.cycling("{1}{B}"))

    triggeredAbility {
        trigger = OnCycle(controllerOnly = true)
        target = Targets.Creature
        effect = MayEffect(GrantKeywordUntilEndOfTurnEffect(Keyword.FEAR, EffectTarget.ContextTarget(0)))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "138"
        artist = "Heather Hudson"
        flavorText = "It puts the \"fun\" in \"funeral.\""
        imageUri = "https://cards.scryfall.io/large/front/8/4/8496e9c2-4c13-4307-bda7-b88512a21a6a.jpg?1562926208"
    }
}
