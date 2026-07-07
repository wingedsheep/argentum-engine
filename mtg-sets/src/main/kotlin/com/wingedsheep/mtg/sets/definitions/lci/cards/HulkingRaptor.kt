package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost

/**
 * Hulking Raptor — The Lost Caverns of Ixalan #191
 * {2}{G}{G} · Creature — Dinosaur · 5/3 · Rare
 *
 * Ward {2}
 * At the beginning of your first main phase, add {G}{G}.
 *
 * Ward {2}: `KeywordAbility.Ward(WardCost.Mana("{2}"))` — triggers whenever an opponent
 * targets this creature; they must pay {2} or the spell/ability is countered.
 *
 * First-main mana: `Triggers.FirstMainPhase` fires at the start of the controller's
 * precombat main phase; `Effects.AddMana(Color.GREEN, 2)` adds two unrestricted {G} to
 * the controller's mana pool.
 */
val HulkingRaptor = card("Hulking Raptor") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dinosaur"
    power = 5
    toughness = 3
    oracleText = "Ward {2}\nAt the beginning of your first main phase, add {G}{G}."

    // Ward {2}: opponents must pay {2} to keep a spell/ability targeting this creature.
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))

    // At the beginning of your first main phase, add {G}{G}.
    triggeredAbility {
        trigger = Triggers.FirstMainPhase
        effect = Effects.AddMana(Color.GREEN, 2)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "191"
        artist = "Néstor Ossandón Leal"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/45f763af-5a6a-404c-8e8c-4dbed71277bc.jpg?1782694456"
    }
}
