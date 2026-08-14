package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * An Unexpected Party // At the Door — The Hobbit #29
 * {2}{W}{W}
 * Enchantment
 *
 * As this enchantment enters, choose a creature type.
 * Creatures you control of the chosen type get +2/+2.
 *
 * Adventure: At the Door — {X}{2}{W}, Sorcery — Adventure
 * Create X 2/2 red Dwarf creature tokens.
 *
 * The type is picked as the enchantment enters — a replacement effect ([EntersWithChoice]), not a
 * resolution-time trigger — so the lord is already live the first time state-based actions look
 * (CR 614.12, Adaptive Automaton). The buff is a plain Layer 7c [ModifyStats] over
 * `ChosenSubtypeCreatures`, so creatures of the chosen type that arrive later are covered too.
 *
 * The Adventure's X is the {X} in *its* mana cost, read at resolution as [DynamicAmount.XValue].
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the caster
 * cast it as the enchantment later from exile.)
 */
val AnUnexpectedParty = card("An Unexpected Party") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "As this enchantment enters, choose a creature type.\n" +
        "Creatures you control of the chosen type get +2/+2."

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    staticAbility {
        ability = ModifyStats(
            powerBonus = 2,
            toughnessBonus = 2,
            filter = GroupFilter.ChosenSubtypeCreatures().youControl()
        )
    }

    adventure("At the Door") {
        manaCost = "{X}{2}{W}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Create X 2/2 red Dwarf creature tokens. " +
            "(Then exile this card. You may cast the enchantment later from exile.)"
        spell {
            effect = Effects.CreateToken(
                count = DynamicAmount.XValue,
                power = 2,
                toughness = 2,
                colors = setOf(Color.RED),
                creatureTypes = setOf("Dwarf"),
                imageUri = "https://cards.scryfall.io/normal/front/9/f/9fcb3a3f-c0d4-43d4-8549-826a38bfa27d.jpg?1786258756",
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "29"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3aa29fe8-1687-486f-b4df-c04977869ab1.jpg?1783902787"
    }
}
