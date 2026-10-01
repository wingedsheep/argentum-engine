package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Unctus's Retrofitter
 * {2}{U}
 * Creature — Phyrexian Artificer
 * 2/3
 *
 * Toxic 1 (Players dealt combat damage by this creature also get a poison counter.)
 * When this creature enters, up to one target artifact you control becomes an artifact creature
 * with base power and toughness 4/4 for as long as this creature remains on the battlefield.
 *
 * The animate adds the creature type (Layer 4) and sets base P/T 4/4 (Layer 7b) without removing
 * abilities, types, or subtypes (per the 2023-02-04 rulings). The source-keyed
 * [Duration.WhileSourceOnBattlefield] means that if the Retrofitter has already left the battlefield
 * when the trigger resolves, the effect never applies.
 */
val UnctussRetrofitter = card("Unctus's Retrofitter") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Phyrexian Artificer"
    power = 2
    toughness = 3
    oracleText = "Toxic 1 (Players dealt combat damage by this creature also get a poison counter.)\n" +
        "When this creature enters, up to one target artifact you control becomes an artifact creature " +
        "with base power and toughness 4/4 for as long as this creature remains on the battlefield."

    keywordAbility(KeywordAbility.Numeric(Keyword.TOXIC, 1))

    triggeredAbility {
        trigger = Triggers.self.enters()
        val artifact = target(TargetFilter.Artifact.youControl(), optional = true)
        effect = Effects.BecomeCreature(
            target = artifact,
            power = 4,
            toughness = 4,
            duration = Duration.WhileSourceOnBattlefield("Unctus's Retrofitter")
        )
        description = "When this creature enters, up to one target artifact you control becomes an " +
            "artifact creature with base power and toughness 4/4 for as long as this creature " +
            "remains on the battlefield."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "76"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/3/0/30225e40-4bde-4e2e-b0f0-48ed16aebafd.jpg?1783918055"
        ruling("2023-02-04", "Unctus's Retrofitter doesn't remove any abilities the target artifact has.")
        ruling("2023-02-04", "The target artifact retains any types, subtypes, and supertypes it has.")
        ruling("2023-02-04", "If the artifact was already a creature, its base power and toughness will become 4/4. This overwrites any previous effects that set its base power and/or toughness to specific values. Any power- or toughness-setting effects that start to apply after Unctus's Retrofitter's ability resolves will overwrite this effect.")
        ruling("2023-02-04", "If Unctus's Retrofitter leaves the battlefield before its enters-the-battlefield ability resolves, that ability will have no effect. The target artifact won't become an artifact creature at all.")
    }
}
