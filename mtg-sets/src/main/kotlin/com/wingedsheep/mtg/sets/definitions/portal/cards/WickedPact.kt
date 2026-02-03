package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DestroyEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.LoseLifeEffect
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Wicked Pact
 * {1}{B}{B}
 * Sorcery
 * Destroy two target nonblack creatures. You lose 5 life.
 */
val WickedPact = card("Wicked Pact") {
    manaCost = "{1}{B}{B}"
    typeLine = "Sorcery"

    spell {
        // Use two separate target requirements so each creature can be targeted independently
        val creature1 = target("first nonblack creature", TargetCreature(
            filter = TargetFilter.Creature.notColor(Color.BLACK)
        ))
        val creature2 = target("second nonblack creature", TargetCreature(
            filter = TargetFilter.Creature.notColor(Color.BLACK)
        ))

        effect = DestroyEffect(creature1) then
                DestroyEffect(creature2) then
                LoseLifeEffect(5, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "117"
        artist = "Ron Spencer"
        flavorText = "Power demands sacrifice."
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4d7c251-cb65-4ffc-8bf0-5e9692004a87.jpg"
    }
}
