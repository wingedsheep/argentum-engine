package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Gate to the Afterlife
 * {3}
 * Artifact
 * Whenever a nontoken creature you control dies, you gain 1 life. Then you may draw a card.
 * If you do, discard a card.
 * {2}, {T}, Sacrifice this artifact: Search your graveyard, hand, and/or library for a card named
 * God-Pharaoh's Gift and put it onto the battlefield. If you search your library this way, shuffle.
 * Activate only if there are six or more creature cards in your graveyard.
 *
 * "Activate only if …" is an activation restriction (checked on activation), not a resolution check.
 */
val GateToTheAfterlife = card("Gate to the Afterlife") {
    manaCost = "{3}"
    typeLine = "Artifact"
    oracleText = "Whenever a nontoken creature you control dies, you gain 1 life. Then you may draw a card. " +
        "If you do, discard a card.\n" +
        "{2}, {T}, Sacrifice this artifact: Search your graveyard, hand, and/or library for a card named " +
        "God-Pharaoh's Gift and put it onto the battlefield. If you search your library this way, shuffle. " +
        "Activate only if there are six or more creature cards in your graveyard."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().nontoken()).dies()
        effect = Effects.GainLife(1) then Effects.May(Patterns.Hand.loot())
        description = "Whenever a nontoken creature you control dies, you gain 1 life. Then you may " +
            "draw a card. If you do, discard a card."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Patterns.Library.searchMultipleZones(
            zones = listOf(Zone.GRAVEYARD, Zone.HAND, Zone.LIBRARY),
            filter = GameObjectFilter.Any.named("God-Pharaoh's Gift"),
            count = 1,
            destination = SearchDestination.BATTLEFIELD
        )
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.CreatureCardsInGraveyardAtLeast(6))
        )
        description = "{2}, {T}, Sacrifice this artifact: Search your graveyard, hand, and/or library " +
            "for a card named God-Pharaoh's Gift and put it onto the battlefield. If you search your " +
            "library this way, shuffle. Activate only if there are six or more creature cards in your graveyard."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "228"
        artist = "Christine Choi"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b544a59-86b9-447d-b54d-b94a262b6866.jpg?1783936453"
        ruling("2017-04-18", "If Gate to the Afterlife is put into a graveyard at the same time a nontoken creature you control dies, its first ability triggers for that creature.")
    }
}
