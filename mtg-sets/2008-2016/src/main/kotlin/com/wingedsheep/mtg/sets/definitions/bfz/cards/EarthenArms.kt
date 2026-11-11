package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Earthen Arms
 * {1}{G}
 * Sorcery
 * Put two +1/+1 counters on target permanent.
 * Awaken 4—{6}{G} (If you cast this spell for {6}{G}, also put four +1/+1 counters on target land
 * you control and it becomes a 0/0 Elemental creature with haste. It's still a land.)
 *
 * The `spell { }` block holds only the printed spell: casting for the awaken cost adds the land
 * target after the permanent target and runs the awaken ability after the counters. The two are
 * separate instances of "target", so one land you control may be chosen for both (ruling) and ends
 * up a 6/6.
 */
val EarthenArms = card("Earthen Arms") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Put two +1/+1 counters on target permanent.\n" +
        "Awaken 4—{6}{G} (If you cast this spell for {6}{G}, also put four +1/+1 counters on target " +
        "land you control and it becomes a 0/0 Elemental creature with haste. It's still a land.)"

    keywordAbility(KeywordAbility.awaken(4, "{6}{G}"))

    spell {
        val t = target(TargetFilter.Permanent)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "172"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/d/a/dadc77d8-ef1c-431d-8716-9583f4fad33f.jpg?1783938189"
        ruling("2015-08-25", "Any permanent can have a +1/+1 counter put on it. Those counters won't have any effect unless that permanent is or becomes a creature.")
        ruling("2015-08-25", "If you cast Earthen Arms for its awaken cost, a land you control can be both targets.")
    }
}
