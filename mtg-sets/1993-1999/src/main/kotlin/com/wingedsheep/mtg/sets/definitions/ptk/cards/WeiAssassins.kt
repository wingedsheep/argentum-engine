package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Wei Assassins
 * {3}{B}{B}
 * Creature — Human Soldier Assassin
 * 3/2
 * When this creature enters, target opponent chooses a creature they control. Destroy that creature.
 */
val WeiAssassins = card("Wei Assassins") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Soldier Assassin"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, target opponent chooses a creature they control. Destroy that creature."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            val creatures = gather(CardSource.ControlledPermanents(opponent.asPlayer, GameObjectFilter.Creature))
            val chosen = chooseExactly(1, from = creatures, chooser = Chooser.TargetPlayer)
            destroy(chosen)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "86"
        artist = "Xu Tan"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b448c08-140f-41d6-aed5-ce9b68efafa9.jpg?1783946113"
    }
}
