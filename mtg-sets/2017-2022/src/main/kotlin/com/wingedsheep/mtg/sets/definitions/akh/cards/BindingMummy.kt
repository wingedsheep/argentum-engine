package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Binding Mummy
 * {1}{W}
 * Creature — Zombie
 * 2/2
 * Whenever another Zombie you control enters, you may tap target artifact or creature.
 *
 * The bare noun "Zombie" means any Zombie *permanent* you control (the [WaywardServant] shape), and
 * "another" is the OTHER binding from [Triggers.another]. The target is chosen as the trigger goes
 * on the stack; the "you may" is asked on resolution.
 */
val BindingMummy = card("Binding Mummy") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Zombie"
    oracleText = "Whenever another Zombie you control enters, you may tap target artifact or creature."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Permanent.withSubtype(Subtype.ZOMBIE).youControl()).enters()
        val permanent = target(TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Creature))
        effect = Effects.May(Effects.Tap(permanent))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "6"
        artist = "Shreya Shetty"
        flavorText = "What the gods decree, the mummies enforce."
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fae54072-ae27-4aae-a2b7-71f4551934f6.jpg"
    }
}
