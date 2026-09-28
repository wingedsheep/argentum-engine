package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Lithomantic Barrage — March of the Machine #152.
 * {R} · Sorcery
 *
 * The colour test is read at resolution against the target ("white and/or blue" is a
 * white-or-blue union, so a white-blue target takes 5 once, not 6).
 */
val LithomanticBarrage = card("Lithomantic Barrage") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "This spell can't be countered.\nLithomantic Barrage deals 1 damage to target creature or planeswalker. It deals 5 damage instead if that target is white and/or blue."

    cantBeCountered = true

    spell {
        val t = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(
                GameObjectFilter.Any.withAnyColor(Color.WHITE, Color.BLUE), t,
            ),
            then = Effects.DealDamage(5, t),
            otherwise = Effects.DealDamage(1, t),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "152"
        artist = "Viko Menezes"
        flavorText = "Nahiri's rage shattered hedrons into deadly, burning ruin."
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c45a5f4a-2174-4885-aa5a-c4c24cc732f0.jpg?1783916986"
        ruling("2023-04-14", "\"This spell can't be countered\" means it can't be countered even by the ward keyword ability. If you cast Lithomantic Barrage targeting a creature or planeswalker with ward, you may still pay the ward cost if you want to, but Lithomantic Barrage won't be countered if you don't.")
        ruling("2023-04-14", "Spells and abilities that counter spells can still target Lithomantic Barrage. Such a spell or ability won't counter Lithomantic Barrage, but any additional effects of that spell or ability will still happen.")
    }
}
