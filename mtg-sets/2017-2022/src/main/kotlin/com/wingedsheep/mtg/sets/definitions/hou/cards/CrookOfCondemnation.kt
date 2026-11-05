package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Crook of Condemnation — Hour of Devastation #159 (canonical printing)
 * {2}
 * Artifact
 * {1}, {T}: Exile target card from a graveyard.
 * {1}, Exile this artifact: Exile all graveyards.
 */
val CrookOfCondemnation = card("Crook of Condemnation") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{1}, {T}: Exile target card from a graveyard.\n{1}, Exile this artifact: Exile all graveyards."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val t = target(TargetFilter.CardInGraveyard)
        effect = Effects.Move(t, Zone.EXILE)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.ExileSelf)
        effect = Effects.Pipeline {
            val allGraveyards = gather(
                CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = Player.Each,
                    filter = GameObjectFilter.Any,
                )
            )
            exile(allGraveyards)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "159"
        artist = "Ben Maier"
        flavorText = "The God-Pharaoh's angels stripped former initiates of their cartouches and the fortifying magic they provided."
        imageUri = "https://cards.scryfall.io/normal/front/7/8/78f6d7c2-ee4f-4cd0-a9a2-d8469ff89291.jpg?1783936004"
    }
}
