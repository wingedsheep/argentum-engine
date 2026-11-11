package com.wingedsheep.mtg.sets.definitions.ice.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Snow-Covered Swamp
 * Basic Snow Land — Swamp
 * ({T}: Add {B}.)
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
val SnowCoveredSwamp = card("Snow-Covered Swamp") {
    manaCost = ""
    colorIdentity = "B"
    typeLine = "Basic Snow Land — Swamp"
    oracleText = "({T}: Add {B}.)"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "372"
        artist = "Douglas Shuler"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65a3c27f-6b15-49b6-ac89-36cfb79b3b54.jpg?1783947447"
        inBooster = false
        ruling("2021-02-05", "Snow is a supertype, not a card type. It has no rules meaning or function by itself, but spells and abilities may refer to it.")
        ruling("2021-02-05", "The {S} symbol is a generic mana symbol. It represents a cost that can be paid by one mana that was produced by a snow source. That mana can be any color or colorless.")
        ruling("2021-02-05", "Snow isn't a type of mana. If an effect says you may spend mana as though it were any type, you can't pay for {S} using mana that wasn't produced by a snow source.")
        ruling("2021-02-05", "Some cards have additional effects for each {S} spent to cast them. You can cast these spells even if you don't spend any snow mana to cast them; their additional effects simply won't do anything.")
        ruling("2021-02-05", "The Kaldheim set doesn't have any cards with mana costs that include {S}, but some previous sets do. If an effect says such a spell costs {1} less to cast, that reduction doesn't apply to any {S} costs. This is also true for activated abilities that include {S} in their activation costs and effects that reduce those costs.")
        ruling("2021-02-05", "In a Limited event (usually Booster Draft or Sealed Deck), you can't add basic snow lands to your card pool as you would other basic lands. You can play with basic snow lands only if you open them in your sealed deck or draft them.")
    }
}
