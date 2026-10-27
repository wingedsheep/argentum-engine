package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Gravebreaker Lamia — Theros Beyond Death #98 (canonical printing)
 * {4}{B} · Enchantment Creature — Snake Lamia · 4/4
 *
 * Lifelink
 * When this creature enters, search your library for a card, put it into your graveyard, then
 * shuffle.
 * Spells you cast from your graveyard cost {1} less to cast.
 *
 * The enters trigger is Entomb's search-to-graveyard; the cost reduction is Patrician Geist's
 * `YouCastFromZones(GRAVEYARD)` reduction, which grants no permission to cast from the graveyard.
 */
val GravebreakerLamia = card("Gravebreaker Lamia") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment Creature — Snake Lamia"
    power = 4
    toughness = 4
    oracleText = "Lifelink\n" +
        "When this creature enters, search your library for a card, put it into your graveyard, then shuffle.\n" +
        "Spells you cast from your graveyard cost {1} less to cast."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Any,
            count = 1,
            destination = SearchDestination.GRAVEYARD
        )
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCastFromZones(setOf(Zone.GRAVEYARD)),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "98"
        artist = "Lius Lasahido"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/408a2073-d068-44bc-b596-5a3a3a446ee1.jpg?1783931567"
        ruling(
            "2020-01-24",
            "Gravebreaker Lamia's last ability doesn't give you permission to cast cards from your graveyard. " +
                "You need something else allowing you to do so, such as the escape keyword ability."
        )
    }
}
