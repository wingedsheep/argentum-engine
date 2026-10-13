package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Enlist
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Guardian of New Benalia — "whenever this creature enlists a creature" is
 * [Triggers.self.enlists], which fires once per paid enlist instance.
 */
val GuardianOfNewBenalia = card("Guardian of New Benalia") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Enlist (As this creature attacks, you may tap a nonattacking creature you control without summoning sickness. When you do, add its power to this creature's until end of turn.)\nWhenever this creature enlists a creature, scry 2.\n" +
        "Discard a card: This creature gains indestructible until end of turn. Tap it."

    staticAbility { ability = Enlist }

    triggeredAbility {
        trigger = Triggers.self.enlists()
        effect = Effects.Scry(2)
    }

    activatedAbility {
        cost = Costs.DiscardCard
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self) then
            Effects.Tap(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "19"
        artist = "Ernanda Souza"
        imageUri = "https://cards.scryfall.io/normal/front/4/3/43da76ee-fec3-4b2e-915d-10cf8d518d2c.jpg?1783921365"
        ruling("2022-09-09", "Guardian of New Benalia's second ability triggers whenever its controller taps another creature for Guardian of New Benalia's enlist ability.")
        ruling("2022-09-09", "You may activate Guardian of New Benalia's last ability even if it's already tapped. You may also activate that ability multiple times in the same turn, even if Guardian of New Benalia already has indestructible.")
    }
}
