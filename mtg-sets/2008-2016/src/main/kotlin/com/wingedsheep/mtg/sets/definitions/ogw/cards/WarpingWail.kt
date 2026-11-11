package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Warping Wail
 * {1}{C}
 * Instant
 * ({C} represents colorless mana.)
 * Choose one —
 * • Exile target creature with power or toughness 1 or less.
 * • Counter target sorcery spell.
 * • Create a 1/1 colorless Eldrazi Scion creature token. It has "Sacrifice this token: Add {C}."
 *
 * A colorless card (not devoid): {C} in its cost must be paid with colorless mana.
 */
val WarpingWail = card("Warping Wail") {
    manaCost = "{1}{C}"
    colorIdentity = ""
    typeLine = "Instant"
    oracleText = "({C} represents colorless mana.)\n" +
        "Choose one —\n" +
        "• Exile target creature with power or toughness 1 or less.\n" +
        "• Counter target sorcery spell.\n" +
        "• Create a 1/1 colorless Eldrazi Scion creature token. It has \"Sacrifice this token: Add {C}.\""

    spell {
        modal(chooseCount = 1) {
            mode("Exile target creature with power or toughness 1 or less") {
                val creature = target(TargetFilter.Creature.powerOrToughnessAtMost(1))
                effect = Effects.Exile(creature)
            }
            mode("Counter target sorcery spell") {
                target(TargetFilter.SorcerySpellOnStack)
                effect = Effects.CounterSpell()
            }
            mode("Create a 1/1 colorless Eldrazi Scion token") {
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
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "12"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2ef4db8-b51c-4f52-84f1-6fee31c4a14c.jpg?1783937928"
    }
}
