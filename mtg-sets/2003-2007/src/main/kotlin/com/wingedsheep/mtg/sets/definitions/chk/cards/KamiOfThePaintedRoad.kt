package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kami of the Painted Road
 * {4}{W}
 * Creature — Spirit
 * 3/3
 * Whenever you cast a Spirit or Arcane spell, this creature gains protection from the color of
 * your choice until end of turn.
 *
 * The colour is chosen on resolution (no target), so `ChooseColorThen` wraps the grant.
 */
val KamiOfThePaintedRoad = card("Kami of the Painted Road") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit"
    oracleText = "Whenever you cast a Spirit or Arcane spell, this creature gains protection from the color of your choice until end of turn."
    power = 3
    toughness = 3
    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withAnySubtype("Spirit", "Arcane"))
        effect = Effects.ChooseColorThen(Effects.GrantProtectionFromChosenColor(EffectTarget.Self))
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "23"
        artist = "Ron Spencer"
        flavorText = "In ancient times, precepts of kami law were inscribed onto bridges as a gesture of respect. During the war, humans regarded them as warnings of where not to travel."
        imageUri = "https://cards.scryfall.io/normal/front/7/0/7068739c-0a6a-49a1-88c3-68d7abc58ee2.jpg?1783944337"
    }
}
