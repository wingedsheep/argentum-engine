package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Prison Realm
 * {2}{W}
 * Enchantment
 * When this enchantment enters, exile target creature or planeswalker an opponent controls until this enchantment leaves the battlefield.
 * When this enchantment enters, scry 1.
 *
 * Banishing Light family: the ETB exile links the card to this enchantment and the leaves trigger
 * returns it under its owner's control. The scry is its own enters trigger, as printed.
 */
val PrisonRealm = card("Prison Realm") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, exile target creature or planeswalker an opponent controls until this enchantment leaves the battlefield.\n" +
        "When this enchantment enters, scry 1."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls()))
        effect = Effects.ExileUntilLeaves(permanent)
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "26"
        artist = "Daarken"
        flavorText = "After millennia to craft victory, Bolas had eternity to contemplate defeat."
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d476e8e5-2377-464b-b2d5-a3cd66ee2e45.jpg?1783933476"
    }
}
