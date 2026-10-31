package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Spinning Wheel
 * {3}
 * Artifact
 *
 * {T}: Add one mana of any color.
 * {5}, {T}: Tap target creature.
 */
val SpinningWheel = card("Spinning Wheel") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Add one mana of any color.\n{5}, {T}: Tap target creature."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana()
        manaAbility = true
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}"), Costs.Tap)
        val t = target(TargetFilter.Creature)
        effect = Effects.Tap(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "234"
        artist = "Aaron Miller"
        flavorText = "\"Though long forgotten, the wheel continued to turn, spinning fate from a dusty attic.\"\n—*Beyond the Great Henge*"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/070ff479-9d87-4ab6-aaaa-e96b9df0bac4.jpg"
    }
}
