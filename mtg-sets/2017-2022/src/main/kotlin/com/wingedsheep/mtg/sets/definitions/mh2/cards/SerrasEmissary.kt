package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantProtectionFromToGroup
import com.wingedsheep.sdk.scripting.GrantProtectionToController
import com.wingedsheep.sdk.scripting.ModeOption
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Serra's Emissary
 * {4}{W}{W}{W}
 * Creature — Angel
 * 7/7
 * Flying
 * As this creature enters, choose a card type.
 * You and creatures you control have protection from the chosen card type.
 *
 * The choice is an as-enters replacement (Stenn, Paranoid Partisan's shape): an
 * [EntersWithChoice] of `ChoiceType.MODE` whose option ids are the card types a normal game can
 * produce (per the ruling), stored in [ChoiceSlot.MODE]. Both protections read that choice through
 * `ProtectionScope.ChosenCardType(ChoiceSlot.MODE)` — one grant for the creatures, one for you.
 */
val SerrasEmissary = card("Serra's Emissary") {
    manaCost = "{4}{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 7
    toughness = 7
    oracleText = "Flying\n" +
        "As this creature enters, choose a card type.\n" +
        "You and creatures you control have protection from the chosen card type."

    keywords(Keyword.FLYING)

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.MODE,
            modeOptions = listOf(
                "Artifact" to "artifacts", "Battle" to "battles", "Creature" to "creatures",
                "Enchantment" to "enchantments", "Instant" to "instants", "Kindred" to "Kindred cards",
                "Land" to "lands", "Planeswalker" to "planeswalkers", "Sorcery" to "sorceries"
            ).map { (type, plural) ->
                ModeOption(
                    id = type,
                    label = type,
                    description = "You and creatures you control have protection from $plural."
                )
            }
        )
    )

    staticAbility {
        ability = GrantProtectionFromToGroup(
            scope = ProtectionScope.ChosenCardType(ChoiceSlot.MODE),
            filter = GroupFilter(GameObjectFilter.Creature.youControl())
        )
    }

    staticAbility {
        ability = GrantProtectionToController(ProtectionScope.ChosenCardType(ChoiceSlot.MODE))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "30"
        artist = "Nils Hamm"
        flavorText = "Where swords fail, faith transcends."
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b6534049-3045-4546-b1e8-e5b1b0df5f56.jpg?1783926884"

        ruling(
            "2021-06-18",
            "You may choose any card type. The ones that may appear in a normal game of Magic are artifact, battle, creature, enchantment, instant, kindred, land, planeswalker, and sorcery. Supertypes such as snow and basic may not be chosen. Subtypes such as Angel, Aura, and Forest also may not be chosen."
        )
        ruling(
            "2021-06-18",
            "The card type choice is a replacement effect, not a triggered ability. It doesn't use the stack and can't be responded to. Notably, if you choose instant, your opponent can't try to cast an instant on the Emissary before it has protection."
        )
    }
}
