package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Venerated Rotpriest
 * {G}
 * Creature — Phyrexian Druid
 * 1/2
 *
 * Toxic 1 (Players dealt combat damage by this creature also get a poison counter.)
 * Whenever a creature you control becomes the target of a spell, target opponent gets a poison counter.
 *
 * The trigger is spells only — abilities targeting your creatures don't count — and it fires
 * whichever player controls the targeting spell.
 */
val VeneratedRotpriest = card("Venerated Rotpriest") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Phyrexian Druid"
    power = 1
    toughness = 2
    oracleText = "Toxic 1 (Players dealt combat damage by this creature also get a poison counter.)\n" +
        "Whenever a creature you control becomes the target of a spell, target opponent gets a poison counter."

    keywordAbility(KeywordAbility.Numeric(Keyword.TOXIC, 1))

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).becomesTarget(spellsOnly = true)
        val opponent = target(Targets.Opponent)
        effect = Effects.AddCounters(CounterType.POISON, 1, opponent)
        description = "Whenever a creature you control becomes the target of a spell, target opponent gets a poison counter."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "192"
        artist = "Brian Valeza"
        flavorText = "\"This was once a peaceful spring, but I wouldn't advise taking a sip.\"\n—Melira"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d1b032e3-14e3-48ba-ab8a-d2b4f8d31a7d.jpg?1783918006"
    }
}
