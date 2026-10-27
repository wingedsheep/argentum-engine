package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Sifter of Skulls — Oath of the Gatewatch #77
 * {3}{B} · Creature — Eldrazi · 4/3
 * Devoid (This card has no color.)
 * Whenever another nontoken creature you control dies, create a 1/1 colorless Eldrazi Scion
 * creature token. It has "Sacrifice this token: Add {C}."
 *
 * The Scion is the set's standard token (Scion Summoner, Brood Monitor).
 */
val SifterOfSkulls = card("Sifter of Skulls") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Eldrazi"
    power = 4
    toughness = 3
    oracleText = "Devoid (This card has no color.)\n" +
        "Whenever another nontoken creature you control dies, create a 1/1 colorless Eldrazi Scion " +
        "creature token. It has \"Sacrifice this token: Add {C}.\" ({C} represents colorless mana.)"

    keywords(Keyword.DEVOID)

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.nontoken().youControl()).dies()
        effect = Effects.CreateToken(
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
        rarity = Rarity.RARE
        collectorNumber = "77"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be57a5aa-44f1-483f-9274-aa68f5a2bf1f.jpg?1783937914"
    }
}
