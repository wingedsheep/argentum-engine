package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.OnCycle
import com.wingedsheep.sdk.scripting.TapTargetCreaturesEffect
import com.wingedsheep.sdk.scripting.TapUntapEffect
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Choking Tethers
 * {3}{U}
 * Instant
 * Tap up to four target creatures.
 * Cycling {1}{U}
 * When you cycle Choking Tethers, you may tap target creature.
 */
val ChokingTethers = card("Choking Tethers") {
    manaCost = "{3}{U}"
    typeLine = "Instant"

    spell {
        target = TargetCreature(count = 4, optional = true, filter = TargetFilter.Creature)
        effect = TapTargetCreaturesEffect(maxTargets = 4)
    }

    keywordAbility(KeywordAbility.cycling("{1}{U}"))

    triggeredAbility {
        trigger = OnCycle(controllerOnly = true)
        optional = true
        target = TargetCreature(filter = TargetFilter.Creature)
        effect = TapUntapEffect(EffectTarget.ContextTarget(0), tap = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "74"
        artist = "Carl Critchlow"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4de14d1-441f-4d65-bd12-df0506530015.jpg?1562945663"
    }
}
