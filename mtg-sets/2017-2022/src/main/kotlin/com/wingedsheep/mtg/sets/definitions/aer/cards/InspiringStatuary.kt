package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToOwnSpells

/**
 * Inspiring Statuary — Aether Revolt #160 (rare)
 * {3} · Artifact
 *
 * Nonartifact spells you cast have improvise. (Your artifacts can help cast those spells. Each
 * artifact you tap after you're done activating mana abilities pays for {1}.)
 *
 * [GrantKeywordToOwnSpells] over [GameObjectFilter.Nonartifact] — the same runtime grant
 * Ironheart, Clever Champion uses for noncreature spells. Every improvise read site goes through
 * the granted-keyword resolver, so granted improvise behaves exactly like printed improvise,
 * including paying an announced {X}. The Statuary is itself an untapped artifact, so it can be
 * tapped to help pay for the spells it grants improvise to.
 */
val InspiringStatuary = card("Inspiring Statuary") {
    manaCost = "{3}"
    typeLine = "Artifact"
    oracleText = "Nonartifact spells you cast have improvise. (Your artifacts can help cast those " +
        "spells. Each artifact you tap after you're done activating mana abilities pays for {1}.)"

    staticAbility {
        ability = GrantKeywordToOwnSpells(
            keyword = Keyword.IMPROVISE,
            spellFilter = GameObjectFilter.Nonartifact
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "160"
        artist = "Kirsten Zirngibl"
        flavorText = "Ghirapur's most celebrated inventors have been immortalized in clockwork."
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40e2564f-0066-41e0-a767-13ef33a17024.jpg?1783936725"
        ruling(
            "2017-02-09",
            "When using improvise to cast a spell with {X} in its mana cost, first choose the value for X. " +
                "That choice, plus any cost increases or decreases, will determine the spell's total cost. " +
                "Then you can tap artifacts you control to help pay that cost."
        )
        ruling(
            "2017-02-09",
            "If you cast a nonartifact spell that requires you to sacrifice a permanent as an additional " +
                "cost, you may tap that permanent (if it's an artifact) for the spell's improvise ability " +
                "before you sacrifice it to pay that cost."
        )
    }
}
