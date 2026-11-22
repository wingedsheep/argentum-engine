package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Foundry Champion
 * {4}{R}{W}
 * Creature — Elemental Soldier
 * 4/4
 * When this creature enters, it deals damage to any target equal to the number of creatures you
 * control.
 * {R}: This creature gets +1/+0 until end of turn.
 * {W}: This creature gets +0/+1 until end of turn.
 *
 * The creature count is taken as the trigger resolves, so it includes the Champion itself.
 */
val FoundryChampion = card("Foundry Champion") {
    manaCost = "{4}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Elemental Soldier"
    oracleText = "When this creature enters, it deals damage to any target equal to the number of creatures you control.\n" +
        "{R}: This creature gets +1/+0 until end of turn.\n" +
        "{W}: This creature gets +0/+1 until end of turn."
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.enters()
        val anyTarget = target(Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).count(), anyTarget)
    }

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Mana("{W}")
        effect = Effects.ModifyStats(0, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "165"
        artist = "Todd Lockwood"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84e39703-db78-4d3d-aacd-5396848253ed.jpg"
    }
}
