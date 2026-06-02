package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nazgûl
 * {2}{B}
 * Creature — Wraith Knight
 * 1/2
 *
 * Deathtouch
 * When this creature enters, the Ring tempts you.
 * Whenever the Ring tempts you, put a +1/+1 counter on each Wraith you control.
 * A deck can have up to nine cards named Nazgûl.
 */
val Nazgul = card("Nazgûl") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Wraith Knight"
    power = 1
    toughness = 2
    oracleText = "Deathtouch\n" +
        "When this creature enters, the Ring tempts you.\n" +
        "Whenever the Ring tempts you, put a +1/+1 counter on each Wraith you control.\n" +
        "A deck can have up to nine cards named Nazgûl."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.TheRingTemptsYou()
    }

    triggeredAbility {
        trigger = Triggers.RingTemptsYou
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.allCreaturesWithSubtype("Wraith").youControl(),
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "100"
        artist = "Igor Krstic"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/833936c6-9381-4c0b-a81c-4a938be95040.jpg?1739485417"
    }
}
