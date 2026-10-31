package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Harmonious Archon
 * {4}{W}{W}
 * Creature — Archon
 * 4/5
 * Flying
 * Non-Archon creatures have base power and toughness 3/3.
 * When this creature enters, create two 1/1 white Human creature tokens.
 *
 * The static ability sets base P/T in layer 7b (CR 613.4b) for every non-Archon creature on the
 * battlefield, either controller's — so it overwrites earlier base-setting effects and printed or
 * CDA values, while counters and pump effects (7c/7d) still apply on top.
 */
val HarmoniousArchon = card("Harmonious Archon") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Archon"
    power = 4
    toughness = 5
    oracleText = "Flying\nNon-Archon creatures have base power and toughness 3/3.\n" +
        "When this creature enters, create two 1/1 white Human creature tokens."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = SetBasePowerToughnessStatic(
            power = 3,
            toughness = 3,
            filter = GroupFilter(GameObjectFilter.Creature.notSubtype(Subtype.ARCHON))
        )
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            count = 2,
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human"),
            imageUri = "https://cards.scryfall.io/normal/front/9/4/94057dc6-e589-4a29-9bda-90f5bece96c4.jpg?1783932485",
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "17"
        artist = "Anastasia Ovchinnikova"
        flavorText = "First among equals."
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7093834-9627-4da2-9322-c03bfd5b3a71.jpg?1783932673"
        ruling(
            "2019-10-04",
            "Harmonious Archon overwrites all previous effects that set a creature's base power and toughness to " +
                "specific values. Any power- or toughness-setting effects that start to apply after Harmonious " +
                "Archon enters the battlefield will overwrite this effect."
        )
        ruling(
            "2019-10-04",
            "If an effect causes a noncreature permanent to become a creature and sets its power and toughness " +
                "as it does so, that creature will have that power and toughness; it won't be 3/3. Notably, " +
                "crewing a Vehicle does not set its power and toughness, so a Vehicle will be a 3/3 creature once crewed."
        )
        ruling(
            "2019-10-04",
            "Effects that modify a creature's power and/or toughness, such as the effect of Festive Funeral, will " +
                "apply to the creature no matter when they started to take effect. The same is true for any " +
                "counters that change its power and/or toughness."
        )
        ruling(
            "2019-10-04",
            "Because damage remains marked on a creature until the damage is removed as the turn ends, nonlethal " +
                "damage dealt to creatures may become lethal if Harmonious Archon enters or leaves the battlefield " +
                "during that turn."
        )
    }
}
