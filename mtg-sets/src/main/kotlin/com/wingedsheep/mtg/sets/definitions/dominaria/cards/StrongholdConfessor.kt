package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stronghold Confessor
 * {B}
 * Creature — Human Cleric
 * 1/1
 * Kicker {3}
 * Menace
 * If this creature was kicked, it enters with two +1/+1 counters on it.
 */
val StrongholdConfessor = card("Stronghold Confessor") {
    manaCost = "{B}"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 1
    oracleText = "Kicker {3}\nMenace\nIf this creature was kicked, it enters with two +1/+1 counters on it."

    keywordAbility(KeywordAbility.Kicker(ManaCost.parse("{3}")))
    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = WasKicked
        effect = Effects.AddCounters("+1/+1", 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab3fcc43-839b-48c1-91e3-7cc80d8c7f9a.jpg?1562741080"
    }
}
