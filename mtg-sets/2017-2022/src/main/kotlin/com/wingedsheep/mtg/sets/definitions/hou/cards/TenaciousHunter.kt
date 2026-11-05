package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/** "As long as a creature has a -1/-1 counter on it" — any creature, controlled by any player. */
private val creatureHasMinusCounter = Conditions.AnyPlayerControls(
    GameObjectFilter.Creature.withCounter(CounterType.MINUS_ONE_MINUS_ONE)
)

/**
 * Tenacious Hunter — Hour of Devastation #136 (canonical printing)
 * {2}{G}{G} · Creature — Crocodile · 4/4
 *
 * As long as a creature has a -1/-1 counter on it, this creature has vigilance and deathtouch.
 *
 * Two layer-6 self [GrantKeyword]s gated by a battlefield-wide existence check (either player's
 * creatures, including this one).
 */
val TenaciousHunter = card("Tenacious Hunter") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Crocodile"
    power = 4
    toughness = 4
    oracleText = "As long as a creature has a -1/-1 counter on it, this creature has vigilance and deathtouch."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter.source()),
            condition = creatureHasMinusCounter,
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.DEATHTOUCH, GroupFilter.source()),
            condition = creatureHasMinusCounter,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "136"
        artist = "Zezhou Chen"
        flavorText = "The crocodiles of the Broken Lands, long used to feeding on undead flesh, eagerly flooded into Naktamun for a taste of something fresher."
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3eec0010-52ab-4bf2-b589-e7fd97c290fe.jpg?1783936012"
        ruling("2017-07-14", "A creature with a -1/-1 counter on it controlled by any player satisfies Tenacious Hunter’s ability.")
        ruling("2017-07-14", "If Tenacious Hunter loses vigilance after attacking, it will remain untapped.")
    }
}
