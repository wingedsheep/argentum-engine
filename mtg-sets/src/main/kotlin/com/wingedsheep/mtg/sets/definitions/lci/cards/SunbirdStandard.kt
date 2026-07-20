package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.craft
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Sunbird Standard // Sunbird Effigy (CR 702.167, The Lost Caverns of Ixalan)
 * {3}
 * Artifact // Artifact Creature — Bird Construct
 *
 * Front face — Sunbird Standard ({3}, Artifact)
 *   {T}: Add one mana of any color.
 *   Craft with one or more {5} ({5}, Exile this artifact, Exile one or more other
 *   permanents you control and/or cards from your graveyard: Return this card
 *   transformed under its owner's control. Craft only as a sorcery.)
 *
 * Back face — Sunbird Effigy (Artifact Creature — Bird Construct, &#42;/&#42;)
 *   Flying, vigilance, haste
 *   Sunbird Effigy's power and toughness are each equal to the number of colors
 *   among the exiled cards used to craft it.
 *   {T}: For each color among the exiled cards used to craft this creature, add
 *   one mana of that color.
 *
 * Implementation: the front face's `craft(...)` helper wires the activated ability with an
 * [com.wingedsheep.sdk.scripting.AbilityCost.Craft] material cost ([GameObjectFilter.Any],
 * unbounded `minCount = 1` / `maxCount = null` — any permanents you control and/or cards in
 * your graveyard) paired with the {5} mana cost; resolution returns the source transformed
 * with a `CraftedFromExiledComponent` recording the materials. The front's any-color mana
 * ability is [Effects.AddAnyColorMana]. The back face's P/T CDA is
 * [DynamicAmount.CraftedMaterialsColorCount] (distinct printed colors of the exiled
 * materials, 0–5) for both power and toughness, and its mana ability is
 * [Effects.AddOneManaOfEachCraftedMaterialColor] — one mana of each of those colors, no
 * choice involved.
 */

private val SunbirdStandardFront = card("Sunbird Standard") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Add one mana of any color.\n" +
        "Craft with one or more {5} ({5}, Exile this artifact, Exile one or more other permanents you control and/or cards from your graveyard: Return this card transformed under its owner's control. Craft only as a sorcery.)"

    // {T}: Add one mana of any color.
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    // Craft with one or more {5} — any permanents you control and/or cards from your
    // graveyard, one or more of them (the craft cost already excludes the source itself).
    craft(
        filter = GameObjectFilter.Any,
        cost = "{5}",
        materialDescription = "one or more",
        minCount = 1,
        maxCount = null
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "262"
        artist = "Aldo Domínguez"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0b6d40a-fded-4625-b03c-765c88d75766.jpg?1782694401"
    }
}

private val SunbirdEffigy = card("Sunbird Effigy") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Artifact Creature — Bird Construct"
    // P/T CDA: each equal to the number of colors among the cards exiled to craft it
    // (CR 702.167c). Reads CraftedFromExiledComponent on this entity each projection pass.
    dynamicStats(DynamicAmount.CraftedMaterialsColorCount)
    oracleText = "Flying, vigilance, haste\n" +
        "Sunbird Effigy's power and toughness are each equal to the number of colors among the exiled cards used to craft it.\n" +
        "{T}: For each color among the exiled cards used to craft this creature, add one mana of that color."

    keywords(Keyword.FLYING, Keyword.VIGILANCE, Keyword.HASTE)

    // {T}: For each color among the exiled cards used to craft this creature, add one
    // mana of that color. Fixed output (0-5 mana), no color choice.
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddOneManaOfEachCraftedMaterialColor()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "262"
        artist = "Aldo Domínguez"
        imageUri = "https://cards.scryfall.io/normal/back/e/0/e0b6d40a-fded-4625-b03c-765c88d75766.jpg?1782694401"
    }
}

val SunbirdStandard: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = SunbirdStandardFront,
    backFace = SunbirdEffigy
)
