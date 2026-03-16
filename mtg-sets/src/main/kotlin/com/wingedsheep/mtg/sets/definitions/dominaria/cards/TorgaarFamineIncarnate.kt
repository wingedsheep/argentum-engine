package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Torgaar, Famine Incarnate
 * {6}{B}{B}
 * Legendary Creature — Avatar
 * 7/6
 * As an additional cost to cast this spell, you may sacrifice any number of creatures.
 * This spell costs {2} less to cast for each creature sacrificed this way.
 * When Torgaar enters, up to one target player's life total becomes half their starting
 * life total, rounded down.
 */
val TorgaarFamineIncarnate = card("Torgaar, Famine Incarnate") {
    manaCost = "{6}{B}{B}"
    typeLine = "Legendary Creature — Avatar"
    power = 7
    toughness = 6
    oracleText = "As an additional cost to cast this spell, you may sacrifice any number of creatures. This spell costs {2} less to cast for each creature sacrificed this way.\nWhen Torgaar, Famine Incarnate enters, up to one target player's life total becomes half their starting life total, rounded down."

    additionalCost(AdditionalCost.SacrificeCreaturesForCostReduction())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val player = target("player", TargetPlayer(optional = true))
        effect = Effects.SetLifeTotal(10, player)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "108"
        artist = "Lius Lasahido"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/cab46d5c-95dd-47a0-9f96-dde07d2f8b81.jpg?1562742911"
    }
}
