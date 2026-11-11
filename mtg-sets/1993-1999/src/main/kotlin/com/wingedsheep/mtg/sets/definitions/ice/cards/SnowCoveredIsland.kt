package com.wingedsheep.mtg.sets.definitions.ice.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Snow-Covered Island
 * Basic Snow Land — Island
 * ({T}: Add {U}.)
 *
 * Written with the plain [card] DSL rather than the `basicLand` helper: that helper hardcodes the
 * "Basic Land — <type>" type line and cannot carry the Snow supertype. The intrinsic mana ability is
 * spelled out here to match what `basicLand` would have generated.
 *
 * `CardDiscovery.findBasicLandsIn` files this into Ice Age's `basicLands` (it is a Basic land), but
 * a snow basic is never a land-station basic (`TypeLine.isFreelyAddedBasicLand`). `inBooster = false`
 * keeps it out of Ice Age's own booster pool, where a set's pack-only basics would otherwise join
 * its commons; Kaldheim's land slot reaches snow basics through its reprint rows instead.
 */
val SnowCoveredIsland = card("Snow-Covered Island") {
    manaCost = ""
    colorIdentity = "U"
    typeLine = "Basic Snow Land — Island"
    oracleText = "({T}: Add {U}.)"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "371"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad8b77cf-b53e-4da3-9c27-3851b7b25a98.jpg?1783947448"
        inBooster = false
        ruling("2021-02-05", "Snow is a supertype, not a card type. It has no rules meaning or function by itself, but spells and abilities may refer to it.")
        ruling("2021-02-05", "The {S} symbol is a generic mana symbol. It represents a cost that can be paid by one mana that was produced by a snow source. That mana can be any color or colorless.")
        ruling("2021-02-05", "Snow isn't a type of mana. If an effect says you may spend mana as though it were any type, you can't pay for {S} using mana that wasn't produced by a snow source.")
        ruling("2021-02-05", "Some cards have additional effects for each {S} spent to cast them. You can cast these spells even if you don't spend any snow mana to cast them; their additional effects simply won't do anything.")
        ruling("2021-02-05", "The Kaldheim set doesn't have any cards with mana costs that include {S}, but some previous sets do. If an effect says such a spell costs {1} less to cast, that reduction doesn't apply to any {S} costs. This is also true for activated abilities that include {S} in their activation costs and effects that reduce those costs.")
        ruling("2021-02-05", "In a Limited event (usually Booster Draft or Sealed Deck), you can't add basic snow lands to your card pool as you would other basic lands. You can play with basic snow lands only if you open them in your sealed deck or draft them.")
    }
}
