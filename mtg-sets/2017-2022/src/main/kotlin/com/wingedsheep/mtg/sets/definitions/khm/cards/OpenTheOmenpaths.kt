package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Open the Omenpaths (Kaldheim #143)
 * {2}{R} · Instant
 *
 * Choose one —
 * • Add two mana of any one color and two mana of any other color. Spend this mana only to cast
 *   creature or enchantment spells.
 * • Creatures you control get +1/+0 until end of turn.
 *
 * The first mode chooses a color, adds two of it, then adds two of a color picked from the other
 * four ([ManaColorSet.ExceptChosenColor]) — two distinct colors, as printed.
 */
private val creatureOrEnchantmentSpells = ManaRestriction.AnyOf(
    listOf(
        ManaRestriction.CardTypeSpellsOrAbilitiesOnly(CardType.CREATURE, allowSpells = true, allowAbilities = false),
        ManaRestriction.CardTypeSpellsOrAbilitiesOnly(CardType.ENCHANTMENT, allowSpells = true, allowAbilities = false),
    )
)

val OpenTheOmenpaths = card("Open the Omenpaths") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Add two mana of any one color and two mana of any other color. Spend this mana only to cast " +
        "creature or enchantment spells.\n" +
        "• Creatures you control get +1/+0 until end of turn."

    spell {
        modal(chooseCount = 1) {
            mode("Add two mana of any one color and two mana of any other color") {
                effect = Effects.ChooseColorThen(
                    Effects.AddManaOfChoice(ManaColorSet.AnyColor, 2, creatureOrEnchantmentSpells) then
                        Effects.AddManaOfChoice(ManaColorSet.ExceptChosenColor(), 2, creatureOrEnchantmentSpells),
                    prompt = "Choose a color (two mana of it, then two of another color)"
                )
            }
            mode("Creatures you control get +1/+0 until end of turn") {
                effect = Patterns.Group.modifyStatsForAll(1, 0, Filters.Group.creaturesYouControl)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "143"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e4023c8-8e7f-42b9-99e5-87e80fc3d6c8.jpg?1783928227"
    }
}
