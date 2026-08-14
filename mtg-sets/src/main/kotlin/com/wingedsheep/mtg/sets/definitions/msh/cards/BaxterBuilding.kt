package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Baxter Building
 * Land
 * {T}: Add {C}.
 * {4}, {T}: Add four mana in any combination of colors.
 * {4}, {T}: Draw a card. Activate only if you control a creature with toughness 4 or greater.
 *
 * Implementation note: "four mana in any combination of colors" is
 * [Effects.AddManaInAnyCombination] — each pip's colour is chosen independently at resolution,
 * unlike [Effects.AddManaOfChoice] which produces one colour for the whole activation. The draw
 * is gated by an [ActivationRestriction.OnlyIfCondition] so it can't even be activated without a
 * toughness-4 creature (the toughness test reads projected battlefield state).
 */
val BaxterBuilding = card("Baxter Building") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{4}, {T}: Add four mana in any combination of colors.\n" +
        "{4}, {T}: Draw a card. Activate only if you control a creature with toughness 4 or greater."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        effect = Effects.AddManaInAnyCombination(4)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        effect = Effects.DrawCards(1)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.YouControl(GameObjectFilter.Creature.toughnessAtLeast(4))
            )
        )
        description = "Draw a card. Activate only if you control a creature with toughness 4 or greater."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "261"
        artist = "Paulius Daščioras"
        flavorText = "\"Home sweet home, until it gets teleported to the Negative Zone again or " +
            "somethin'.\"\n—The Thing, Ben Grimm"
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1abc652f-8e9d-4df7-bc4e-d8b515a40fec.jpg?1783902888"
    }
}
