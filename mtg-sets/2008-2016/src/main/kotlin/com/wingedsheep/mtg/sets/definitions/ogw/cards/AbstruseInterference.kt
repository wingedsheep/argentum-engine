package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Abstruse Interference
 * {2}{U}
 * Instant
 * Devoid (This card has no color.)
 * Counter target spell unless its controller pays {1}. You create a 1/1 colorless Eldrazi Scion
 * creature token. It has "Sacrifice this token: Add {C}."
 *
 * The tax counter and the Scion are sequenced, not linked: the token is created whether or not the
 * spell's controller pays (Flip the Switch's shape).
 */
val AbstruseInterference = card("Abstruse Interference") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Devoid (This card has no color.)\n" +
        "Counter target spell unless its controller pays {1}. You create a 1/1 colorless Eldrazi Scion " +
        "creature token. It has \"Sacrifice this token: Add {C}.\" ({C} represents colorless mana.)"

    keywords(Keyword.DEVOID)

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterUnlessPays("{1}") then Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Eldrazi", "Scion"),
            activatedAbilities = listOf(
                ActivatedAbility(
                    cost = Costs.SacrificeSelf,
                    effect = Effects.AddColorlessMana(1),
                    isManaAbility = true,
                    timing = TimingRule.ManaAbility,
                    descriptionOverride = "Sacrifice this token: Add {C}.",
                )
            ),
            imageUri = "https://cards.scryfall.io/normal/front/a/0/a03edf14-3495-4f61-ad73-f16e9456472c.jpg?1783937863",
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "40"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/249a7be3-311e-4ce6-97dc-97242463ae23.jpg?1783937922"

        ruling("2016-01-22", "You get the Eldrazi Scion even if the controller of the spell pays {1}.")
    }
}
