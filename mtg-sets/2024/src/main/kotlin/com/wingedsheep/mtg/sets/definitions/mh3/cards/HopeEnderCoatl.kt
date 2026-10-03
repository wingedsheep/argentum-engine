package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hope-Ender Coatl — Modern Horizons 3 #64 (uncommon)
 * {2}{U} · Creature — Eldrazi Snake · 2/2
 *
 * Devoid
 * Flash
 * When you cast this spell, counter target spell an opponent controls unless they pay {1}.
 * Flying
 *
 * The cast trigger resolves before the Coatl itself (CR 603.2), so the Coatl can be flashed in
 * as a soft counter. "They" is the targeted spell's controller, who is the one asked to pay.
 */
val HopeEnderCoatl = card("Hope-Ender Coatl") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Eldrazi Snake"
    power = 2
    toughness = 2
    oracleText = "Devoid (This card has no color.)\n" +
        "Flash\n" +
        "When you cast this spell, counter target spell an opponent controls unless they pay {1}.\n" +
        "Flying"

    keywords(Keyword.DEVOID, Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        val spell = target(TargetFilter(GameObjectFilter.Any.opponentControls(), zone = Zone.STACK))
        trigger = Triggers.self.isCast()
        effect = Effects.CounterUnlessPays("{1}")
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Filip Burburan"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/26973cad-26d7-4d42-a58a-85c3dce3b9fd.jpg?1783911290"
        ruling(
            "2024-06-07",
            "Hope-Ender Coatl's triggered ability will resolve before Hope-Ender Coatl does. If " +
                "Hope-Ender Coatl is countered or otherwise leaves the stack in response to that " +
                "triggered ability, the triggered ability will still resolve as normal.",
        )
    }
}
