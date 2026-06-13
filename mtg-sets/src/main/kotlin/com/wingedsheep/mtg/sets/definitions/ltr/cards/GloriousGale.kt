package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect

/**
 * Glorious Gale
 * {1}{U}
 * Instant
 *
 * Counter target creature spell. If it was a legendary spell, the Ring tempts you.
 *
 * Composable: the legendary check is evaluated while the spell is still on the stack (legendary-ness
 * is intrinsic, so ordering the conditional before the counter is functionally identical to the
 * printed order), then the spell is countered.
 */
val GloriousGale = card("Glorious Gale") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target creature spell. If it was a legendary spell, the Ring tempts you."

    spell {
        target("creature spell", Targets.CreatureSpell)
        effect = ConditionalEffect(
            condition = Conditions.TargetMatchesFilter(GameObjectFilter.Any.legendary(), 0),
            effect = Effects.TheRingTemptsYou()
        ).then(Effects.CounterSpell())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "51"
        artist = "Ekaterina Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e1057de-5710-415c-9a51-1d8bd86021a3.jpg?1686968102"
    }
}
