package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Wicked Akuba
 * {B}{B}
 * Creature — Spirit
 * 2/2
 * {B}: Target player dealt damage by this creature this turn loses 1 life.
 */
val WickedAkuba = card("Wicked Akuba") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Spirit"
    oracleText = "{B}: Target player dealt damage by this creature this turn loses 1 life."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Mana("{B}")
        val player = target(
            TargetPlayer(
                restriction = Conditions.candidateWasDealtDamageBySourceThisTurn(),
                descriptionOverride = "target player dealt damage by this creature this turn",
            )
        )
        effect = Effects.LoseLife(1, player)
        description = "{B}: Target player dealt damage by this creature this turn loses 1 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "150"
        artist = "Ittoku"
        flavorText = "The sound of children weeping is a song that fills its heart with joy."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3172965-6057-472f-9712-1dd23d25d1a7.jpg?1783944306"
    }
}
