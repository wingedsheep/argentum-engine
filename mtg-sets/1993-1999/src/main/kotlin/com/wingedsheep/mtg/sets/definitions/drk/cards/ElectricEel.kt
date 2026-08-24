package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Electric Eel
 * {U}
 * Creature — Fish
 * 1/1
 * When this creature enters, it deals 1 damage to you.
 * {R}{R}: This creature gets +2/+0 until end of turn and deals 1 damage to you.
 */
val ElectricEel = card("Electric Eel") {
    manaCost = "{U}"
    colorIdentity = "UR"
    typeLine = "Creature — Fish"
    oracleText = "When this creature enters, it deals 1 damage to you.\n" +
        "{R}{R}: This creature gets +2/+0 until end of turn and deals 1 damage to you."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You))
    }

    activatedAbility {
        cost = Costs.Mana("{R}{R}")
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
            .then(Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You)))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "25"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b8834c18-0e4e-4785-9d15-b33345e3789b.jpg?1783947944"
    }
}
