package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Allied Assault — Zendikar Rising #1
 * {2}{W} · Instant
 *
 * Up to two target creatures each get +X/+X until end of turn, where X is the number of creatures
 * in your party.
 *
 * "Up to two target creatures **each**" is a plural requirement, so the pump runs once per chosen
 * target ([ForEachTargetEffect] over `ContextTarget(0)`). X is read as the spell resolves and the
 * resulting modifier is fixed for the turn.
 */
val AlliedAssault = card("Allied Assault") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Up to two target creatures each get +X/+X until end of turn, where X is the number of " +
        "creatures in your party. (Your party consists of up to one each of Cleric, Rogue, Warrior, and Wizard.)"

    spell {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.ModifyStats(DynamicAmounts.partySize(), DynamicAmounts.partySize(), EffectTarget.ContextTarget(0))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "1"
        artist = "Josh Hass"
        flavorText = "A common foe is the death of old hatreds."
        imageUri = "https://cards.scryfall.io/normal/front/3/4/340df0d4-9848-4372-9f45-a73a8e94058a.jpg?1783929425"
        ruling(
            "2020-09-25",
            "The value of X is determined only as Allied Assault resolves. Once that happens, the value of X " +
                "won't change later in the turn even if the number of creatures in your party changes."
        )
    }
}
