package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.CompanionCondition
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModeOption
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Umori, the Collector — Ikoria: Lair of Behemoths #231
 * {2}{B/G}{B/G} · Legendary Creature — Ooze · 4/5
 *
 * Companion — one card type must be common to every nonland card in the starting deck
 * (`ShareACardType(Nonland)`). "As Umori enters, choose a card type" is a true as-enters choice
 * (Stenn's shape): an [EntersWithChoice] of `ChoiceType.MODE` whose option ids are card-type
 * names, read by the cost static through `ofChosenCardTypeComponent(ChoiceSlot.MODE)`. Unlike
 * Stenn, any card type may be chosen — creature and land included (land is legal but reduces
 * nothing, since lands are never cast). The reduction is generic-only, per the ruling.
 */
val UmoriTheCollector = card("Umori, the Collector") {
    manaCost = "{2}{B/G}{B/G}"
    typeLine = "Legendary Creature — Ooze"
    power = 4
    toughness = 5
    oracleText = "Companion — Each nonland card in your starting deck shares a card type. " +
        "(If this card is your chosen companion, you may put it into your hand from outside the game " +
        "for {3} as a sorcery.)\n" +
        "As Umori enters, choose a card type.\n" +
        "Spells you cast of the chosen type cost {1} less to cast."

    keywordAbility(KeywordAbility.companion(CompanionCondition.ShareACardType(GameObjectFilter.Nonland)))

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.MODE,
            modeOptions = listOf(
                "Artifact", "Battle", "Creature", "Enchantment", "Instant",
                "Kindred", "Land", "Planeswalker", "Sorcery",
            ).map { type ->
                ModeOption(
                    id = type,
                    label = type,
                    description = "$type spells you cast cost {1} less to cast."
                )
            }
        )
    )

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.ofChosenCardTypeComponent(ChoiceSlot.MODE)),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "231"
        artist = "Jehan Choo"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75ac31e0-ac70-4ee6-b2b1-cc445ffa1da9.jpg?1783931008"
        ruling("2020-04-17", "Land cards in your deck may have any number of additional types that may be shared with other cards or not.")
        ruling("2020-04-17", "Effects that reduce the generic mana cost of a spell (such as that of Umori) can't reduce that spell's colored mana requirements.")
        ruling("2020-04-17", "To satisfy Umori's companion requirement, there must be one card type that each nonland card in your starting deck has. For example, if every nonland card is an artifact creature, enchantment creature, or creature, it is satisfied; but if you have an artifact creature, an artifact, and a creature, it is not satisfied.")
    }
}
