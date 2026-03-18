package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Helm of the Host
 * {4}
 * Legendary Artifact — Equipment
 * At the beginning of combat on your turn, create a token that's a copy of equipped
 * creature, except the token isn't legendary. That token gains haste.
 * Equip {5}
 */
val HelmOfTheHost = card("Helm of the Host") {
    manaCost = "{4}"
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "At the beginning of combat on your turn, create a token that's a copy of equipped creature, except the token isn't legendary. That token gains haste.\nEquip {5}"

    triggeredAbility {
        trigger = Triggers.BeginCombat
        effect = Effects.CreateTokenCopyOfEquippedCreature(
            removeLegendary = true,
            grantHaste = true
        )
    }

    activatedAbility {
        cost = Costs.Mana("{5}")
        timing = TimingRule.SorcerySpeed
        val creature = target("creature you control", Targets.CreatureYouControl)
        effect = Effects.AttachEquipment(creature)
    }

    equipAbility("{5}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "217"
        artist = "Igor Kieryluk"
        flavorText = "Forged out of flowstone for the queen of Vesuva."
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1d65d20c-09e5-4139-838b-7e0e48eb2b2b.jpg?1666094567"
    }
}
