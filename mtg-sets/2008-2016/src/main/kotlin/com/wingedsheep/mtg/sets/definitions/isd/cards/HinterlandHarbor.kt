package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Hinterland Harbor
 * Land
 * This land enters tapped unless you control a Forest or an Island.
 * {T}: Add {G} or {U}.
 */
val HinterlandHarbor = card("Hinterland Harbor") {
    typeLine = "Land"
    colorIdentity = "UG"
    oracleText = "This land enters tapped unless you control a Forest or an Island.\n{T}: Add {G} or {U}."

    replacementEffect(EntersTapped(
        unlessCondition = Conditions.Any(
            Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Forest")),
            Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Island"))
        )
    ))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        flavorText = "In places where the sea meets the trees, it's easy to forget the darkness that rules most of Innistrad."
        rarity = Rarity.RARE
        collectorNumber = "241"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72f15306-56fe-4643-bb4c-4c7c12378d01.jpg"
    }
}
