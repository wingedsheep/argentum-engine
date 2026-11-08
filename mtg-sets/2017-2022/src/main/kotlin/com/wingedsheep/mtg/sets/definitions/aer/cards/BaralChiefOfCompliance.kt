package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Baral, Chief of Compliance
 * {1}{U}
 * Legendary Creature — Human Wizard
 * 1/3
 * Instant and sorcery spells you cast cost {1} less to cast.
 * Whenever a spell or ability you control counters a spell, you may draw a card. If you do, discard a card.
 *
 * The trigger fires only when a spell is actually countered — not for a spell that can't be
 * countered — and it triggers once per spell countered.
 */
val BaralChiefOfCompliance = card("Baral, Chief of Compliance") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Wizard"
    power = 1
    toughness = 3
    oracleText = "Instant and sorcery spells you cast cost {1} less to cast.\n" +
        "Whenever a spell or ability you control counters a spell, you may draw a card. If you do, discard a card."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.InstantOrSorcery),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    triggeredAbility {
        trigger = Triggers.you.countersASpell()
        effect = Effects.May(Patterns.Hand.loot())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "Wesley Burt"
        flavorText = "\"The echo of silence is music to my ears.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/0/60e16d94-1166-4050-8554-686e153a7f80.jpg?1783936774"
    }
}
