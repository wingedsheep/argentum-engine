package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Birthing Hulk — Oath of the Gatewatch #121 (canonical printing)
 * {6}{G} · Creature — Eldrazi Drone · 5/4
 *
 * Devoid (This card has no color.)
 * When this creature enters, create two 1/1 colorless Eldrazi Scion creature tokens. They have
 * "Sacrifice this token: Add {C}." ({C} represents colorless mana.)
 * {1}{C}: Regenerate this creature.
 *
 * Brood Monitor's Scion ETB with count = 2, plus Wolfir Avenger's self-regenerate priced in {1}{C}.
 */
val BirthingHulk = card("Birthing Hulk") {
    manaCost = "{6}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Eldrazi Drone"
    power = 5
    toughness = 4
    oracleText = "Devoid (This card has no color.)\n" +
        "When this creature enters, create two 1/1 colorless Eldrazi Scion creature tokens. " +
        "They have \"Sacrifice this token: Add {C}.\" ({C} represents colorless mana.)\n" +
        "{1}{C}: Regenerate this creature."

    keywords(Keyword.DEVOID)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Eldrazi", "Scion"),
            count = 2,
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

    activatedAbility {
        cost = Costs.Mana("{1}{C}")
        effect = Effects.Regenerate(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "121"
        artist = "Jack Wang"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a1a644e0-96e5-4b73-873f-1132948cb21f.jpg?1783937904"
    }
}
