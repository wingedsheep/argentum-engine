package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ManaExpiry

/**
 * Tundra Fumarole
 * {1}{R}{R}
 * Snow Sorcery
 * Tundra Fumarole deals 4 damage to target creature or planeswalker. Add {C} for each {S} spent to
 * cast this spell. Until end of turn, you don't lose this mana as steps and phases end.
 * ({S} is mana from a snow source.)
 *
 * The damage, then a dynamic colourless add counting the snow mana that paid the cast
 * ([DynamicAmounts.snowManaSpent], 0 for a copy or a cast without snow mana). The mana is tagged
 * [ManaExpiry.KEPT_UNTIL_END_OF_TURN], so it survives every step/phase boundary this turn.
 */
val TundraFumarole = card("Tundra Fumarole") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Snow Sorcery"
    oracleText = "Tundra Fumarole deals 4 damage to target creature or planeswalker. " +
        "Add {C} for each {S} spent to cast this spell. Until end of turn, you don't lose this mana " +
        "as steps and phases end. ({S} is mana from a snow source.)"

    spell {
        val t = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.DealDamage(4, t) then
            Effects.AddColorlessMana(DynamicAmounts.snowManaSpent(), expiry = ManaExpiry.KEPT_UNTIL_END_OF_TURN)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "156"
        artist = "Simon Dominic"
        flavorText = "The only thing that smells worse than troll is burnt troll."
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b251aa7c-d011-4b52-ab92-415d536c0182.jpg?1783928221"
    }
}
