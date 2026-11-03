package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Trapjaw Tyrant
 * {3}{W}{W}
 * Creature — Dinosaur
 * 5/5
 *
 * Enrage — Whenever this creature is dealt damage, exile target creature an opponent controls until
 * this creature leaves the battlefield.
 *
 * The "until" duration is the linked-exile pair: [Effects.ExileUntilLeaves] on enrage and a leaves
 * trigger that returns everything it exiled (each enrage adds to the same pile).
 */
val TrapjawTyrant = card("Trapjaw Tyrant") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dinosaur"
    power = 5
    toughness = 5
    oracleText = "Enrage — Whenever this creature is dealt damage, exile target creature an opponent controls " +
        "until this creature leaves the battlefield."

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage()
        val t = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.ExileUntilLeaves(t)
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "29"
        artist = "Chris Rahn"
        flavorText = "\"I would advise you not to struggle.\"\n—Huatli"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2c280c8-3471-4ae1-be96-0f392b095ce2.jpg?1783935330"
    }
}
