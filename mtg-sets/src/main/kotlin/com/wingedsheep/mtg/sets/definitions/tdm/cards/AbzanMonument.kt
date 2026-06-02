package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Abzan Monument — Tarkir: Dragonstorm #238
 * {2} · Artifact
 *
 * When this artifact enters, search your library for a basic Plains, Swamp, or Forest card,
 * reveal it, put it into your hand, then shuffle.
 * {1}{W}{B}{G}, {T}, Sacrifice this artifact: Create an X/X white Spirit creature token, where
 * X is the greatest toughness among creatures you control. Activate only as a sorcery.
 *
 * The ETB is a mandatory single-card library search restricted to basic lands carrying one of
 * the three Abzan basic subtypes, revealed and placed into hand then shuffled (atomic
 * [LibraryPatterns.searchLibrary]). The sacrifice ability mirrors Kin-Tree Invocation: an X/X
 * token whose P/T are read at resolution as the greatest toughness among creatures you control
 * ([DynamicAmounts.battlefield] MAX toughness).
 */
val AbzanMonument = card("Abzan Monument") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, search your library for a basic Plains, Swamp, or Forest card, " +
        "reveal it, put it into your hand, then shuffle.\n" +
        "{1}{W}{B}{G}, {T}, Sacrifice this artifact: Create an X/X white Spirit creature token, where X " +
        "is the greatest toughness among creatures you control. Activate only as a sorcery."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = LibraryPatterns.searchLibrary(
            filter = GameObjectFilter.BasicLand.withAnyOfSubtypes(
                listOf(Subtype.PLAINS, Subtype.SWAMP, Subtype.FOREST)
            ),
            count = 1,
            destination = SearchDestination.HAND,
            shuffleAfter = true,
            reveal = true
        )
        description = "When this artifact enters, search your library for a basic Plains, Swamp, or Forest card, " +
            "reveal it, put it into your hand, then shuffle."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{W}{B}{G}"), Costs.Tap, Costs.SacrificeSelf)
        timing = TimingRule.SorcerySpeed
        effect = Effects.CreateDynamicToken(
            dynamicPower = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxToughness(),
            dynamicToughness = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxToughness(),
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Spirit"),
            imageUri = "https://cards.scryfall.io/normal/front/8/e/8ea4fc2f-95a4-49d0-b06e-b88d19637737.jpg?1743176763"
        )
        description = "{1}{W}{B}{G}, {T}, Sacrifice this artifact: Create an X/X white Spirit creature token, " +
            "where X is the greatest toughness among creatures you control. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "238"
        artist = "Jorge Jacinto"
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2da9024-3b58-4a57-8f7d-4094c193daee.jpg?1743204940"
    }
}
