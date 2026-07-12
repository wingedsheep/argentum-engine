package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetSpell

/**
 * Syphon Essence
 * {2}{U}
 * Instant
 *
 * Counter target creature or planeswalker spell. Create a Blood token.
 */
val SyphonEssence = card("Syphon Essence") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target creature or planeswalker spell. Create a Blood token. (It's an " +
        "artifact with \"{1}, {T}, Discard a card, Sacrifice this token: Draw a card.\")"

    spell {
        target = TargetSpell(
            filter = TargetFilter(GameObjectFilter.CreatureOrPlaneswalker, zone = Zone.STACK)
        )
        effect = Effects.CounterSpell() then Effects.CreateBlood(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "84"
        artist = "Sean Murray"
        flavorText = "The third step in creating a skaab requires draining the body of blood and " +
            "replacing it with viscus vitae, a mixture of angel's blood and lamp oil."
        imageUri = "https://cards.scryfall.io/normal/front/4/3/435a2d31-ac2c-45aa-8369-6c2d6fbba4e4.jpg?1782703133"
    }
}
