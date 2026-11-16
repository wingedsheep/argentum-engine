package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dance of the Skywise
 * {1}{U}
 * Instant
 *
 * Until end of turn, target creature you control becomes a blue Dragon Illusion with base power
 * and toughness 4/4, loses all abilities, and gains flying.
 *
 * Turn to Frog's shape plus flying: [Effects.RemoveAllAbilities] then [Effects.GrantKeyword] in
 * layer 6 (the later grant survives the loss), creature types and colour set in layers 4/5 so other
 * card types stay, base 4/4 in layer 7b. A face-down morph that loses its abilities this way has no
 * morph cost and can't be turned face up (ruling); a manifested one still turns up for its mana cost.
 */
val DanceOfTheSkywise = card("Dance of the Skywise") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Until end of turn, target creature you control becomes a blue Dragon Illusion with base " +
        "power and toughness 4/4, loses all abilities, and gains flying."

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.RemoveAllAbilities(creature, Duration.EndOfTurn) then
            Effects.SetCreatureSubtypes(setOf("Dragon", "Illusion"), creature, Duration.EndOfTurn) then
            Effects.ChangeColor(creature, setOf(Color.BLUE), Duration.EndOfTurn) then
            Effects.SetBasePowerAndToughness(4, 4, creature, Duration.EndOfTurn) then
            Effects.GrantKeyword(Keyword.FLYING, creature, Duration.EndOfTurn)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "50"
        artist = "Jack Wang"
        flavorText = "The Ojutai believe in the Great Wheel: those who best serve the dragonlord are destined to be reborn as dragons."
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c548d140-3d81-4b33-9985-87703d316a83.jpg?1783938609"
        ruling("2015-02-25", "The target creature will lose all other colors and creature types and be blue, a Dragon, and an Illusion. It will retain any other types it may have had, such as artifact.")
        ruling("2015-02-25", "The target creature will lose any abilities it may have gained prior to Dance of the Skywise resolving. Notably, if the creature is a face-down creature with morph or megamorph, you can't turn it face up, as it wouldn't have a morph or megamorph cost when face up (because it's lost all abilities other than flying).")
        ruling("2015-02-25", "After Dance of the Skywise resolves, the creature can gain abilities as normal.")
        ruling("2015-02-25", "Dance of the Skywise overrides all previous effects that set the creature's power or toughness to specific values. However, effects that set its power or toughness to specific values that start to apply after Dance of the Skywise resolves will override this effect.")
        ruling("2015-02-25", "Effects that modify the power or toughness of the creature, such as the effects of Giant Growth, will apply to it no matter when they started to take effect. The same is true for counters that change the creature's power or toughness (such as +1/+1 counters) and effects that switch its power and toughness.")
    }
}
