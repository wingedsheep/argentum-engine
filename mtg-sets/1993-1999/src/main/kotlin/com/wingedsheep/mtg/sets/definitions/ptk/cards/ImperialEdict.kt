package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Imperial Edict
 * {1}{B}
 * Sorcery
 * Target opponent chooses a creature they control. Destroy that creature.
 */
val ImperialEdict = card("Imperial Edict") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent chooses a creature they control. Destroy that creature."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            val creatures = gather(CardSource.ControlledPermanents(opponent.asPlayer, GameObjectFilter.Creature))
            val chosen = chooseExactly(1, from = creatures, chooser = Chooser.TargetPlayer)
            destroy(chosen)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "77"
        artist = "Xu Xiaoming"
        flavorText = "Frustrated with Cao Cao's control of the imperial court, Emperor Xian secretly issued an edict condemning him, using his own blood as ink."
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5a7f91d-b4ee-45c1-a229-bb23daf68e6b.jpg?1783946115"
    }
}
