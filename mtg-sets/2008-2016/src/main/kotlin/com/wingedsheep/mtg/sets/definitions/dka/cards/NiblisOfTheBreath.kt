package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Niblis of the Breath
 * {2}{U}
 * Creature — Spirit
 * 2/1
 * Flying
 * {U}, {T}: You may tap or untap target creature.
 */
val NiblisOfTheBreath = card("Niblis of the Breath") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 1
    oracleText = "Flying\n{U}, {T}: You may tap or untap target creature."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U}"), Costs.Tap)
        val creature = target(TargetFilter.Creature)
        effect = Effects.May(
            Effects.Modal(
                modes = listOf(
                    Mode.noTarget(Effects.Tap(creature), "Tap target creature"),
                    Mode.noTarget(Effects.Untap(creature), "Untap target creature")
                ),
                chooseCount = 1,
                countsAsModalSpell = false
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "44"
        artist = "Igor Kieryluk"
        flavorText = "\"Why do the dead remain among us? Where is Flight Alabaster to lead them home? Where is Avacyn?\"\n—Eruth of Lambholt"
        imageUri = "https://cards.scryfall.io/normal/front/0/6/0686843d-6d1e-4488-8c17-7c986a154195.jpg"
    }
}
