package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DestroyEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.LoseLifeEffect
import com.wingedsheep.sdk.targeting.CreatureTargetFilter
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
        val creature1 = target("first creature", TargetCreature(filter = CreatureTargetFilter.NotColor(Color.BLACK)))
        val creature2 = target("second creature", TargetCreature(filter = CreatureTargetFilter.NotColor(Color.BLACK)))
        effect = DestroyEffect(creature1) then
                DestroyEffect(creature2) then
                LoseLifeEffect(5, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "117"
        artist = "Ron Spencer"
        flavorText = "Power demands sacrifice."
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e0f1a2b-8c9d-0e1f-2a3b-4c5d6e7f8a9b.jpg"
    }
}
