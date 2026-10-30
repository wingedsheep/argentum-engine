package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EntersWithCharacteristics
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Dance of the Manse
 * {X}{W}{U}
 * Sorcery
 * Return up to X target artifact and/or non-Aura enchantment cards each with mana value X or less
 * from your graveyard to the battlefield. If X is 6 or more, those permanents are 4/4 creatures in
 * addition to their other types.
 */
val DanceOfTheManse = card("Dance of the Manse") {
    manaCost = "{X}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Sorcery"
    oracleText = "Return up to X target artifact and/or non-Aura enchantment cards each with mana value " +
        "X or less from your graveyard to the battlefield. If X is 6 or more, those permanents are 4/4 " +
        "creatures in addition to their other types."

    spell {
        target = TargetObject(
            optional = true,
            filter = TargetFilter(
                (GameObjectFilter.Artifact or GameObjectFilter.Enchantment.notSubtype(Subtype.AURA))
                    .ownedByYou()
                    .manaValueAtMostDynamic(DynamicAmounts.xValue()),
                zone = Zone.GRAVEYARD,
            ),
            dynamicMaxCount = DynamicAmounts.xValue(),
        )
        // They enter as 4/4 creatures — not noncreature permanents that then become creatures.
        fun returnTargets(entersWith: EntersWithCharacteristics?) = Effects.Pipeline {
            move(
                gather(CardSource.ChosenTargets),
                CardDestination.ToZone(Zone.BATTLEFIELD),
                entersWith = entersWith,
            )
        }
        effect = Effects.If(
            condition = Conditions.CompareAmounts(DynamicAmounts.xValue(), ComparisonOperator.GTE, 6),
            then = returnTargets(EntersWithCharacteristics.creature(4, 4)),
            otherwise = returnTargets(null),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "186"
        artist = "Yeong-Hao Han"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5dca90ef-1c17-4dcc-9fef-dab9ee92f590.jpg?1783932599"

        ruling("2019-10-04", "If a card in a player's graveyard has {X} in its mana cost, that X is considered to be 0.")
        ruling(
            "2019-10-04",
            "If X is 6 or more, the artifacts and/or enchantments enter the battlefield as 4/4 creatures. " +
                "They don't enter as noncreature permanents and then become creatures.",
        )
        ruling("2019-10-04", "An artifact creature or enchantment creature returned this way will be a 4/4 creature.")
        ruling(
            "2019-10-04",
            "An Equipment that's also a creature can't be attached to anything. You can activate its equip " +
                "ability, but it won't become attached.",
        )
        ruling(
            "2019-10-04",
            "If another effect causes one of the returned permanents to become a creature and sets its power " +
                "and toughness as it does so, that creature will have that power and toughness; it won't be 4/4. " +
                "Notably, crewing a Vehicle does not set its power and toughness, so a Vehicle will remain a 4/4 " +
                "creature if you crew it.",
        )
    }
}
