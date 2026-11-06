package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Pacification Array
 * {1}
 * Artifact
 * {2}, {T}: Tap target artifact or creature.
 */
val PacificationArray = card("Pacification Array") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{2}, {T}: Tap target artifact or creature."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val permanent = target(TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Creature))
        effect = Effects.Tap(permanent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "168"
        artist = "Joseph Meehan"
        flavorText = "The Consulate had access to thousands of innovative devices displayed at the Inventors' Fair. A few small changes transformed some of them from marvels of ingenuity to instruments of oppression."
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8bed742a-a12a-4728-a771-07e3d1417419.jpg"
        ruling("2017-02-09", "Players may respond to Pacification Array's ability by activating an ability of the target permanent if that ability's timing permissions allow it. Players may also respond by activating abilities or casting spells whose costs include tapping the target permanent.")
        ruling("2017-02-09", "Once a player has activated an ability with {T} in its cost, Pacification Array can't be used to undo or counter that ability.")
        ruling("2017-02-09", "If you want to stop someone from attacking with a creature by using Pacification Array's ability, you must do so before attackers are declared. You can't wait until after attackers are declared and then try to use it to make a creature stop attacking. Note that your opponent can't start declaring attackers without letting you use your Pacification Array.")
    }
}
