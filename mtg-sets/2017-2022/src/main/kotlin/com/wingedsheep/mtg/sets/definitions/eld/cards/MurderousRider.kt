package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Murderous Rider // Swift End
 * {1}{B}{B}
 * Creature — Zombie Knight
 * 2/3
 * Lifelink
 * When this creature dies, put it on the bottom of its owner's library.
 *
 * Adventure: Swift End — {1}{B}{B}, Instant — Adventure
 * Destroy target creature or planeswalker. You lose 2 life.
 *
 * The death trigger moves the card from the graveyard, so it only finds the card if it's still
 * there when the trigger resolves — [Effects.PutOnBottomOfLibrary] resolves [EffectTarget.Self]
 * to wherever the card actually is and no-ops if it has since moved on. Same shape as Fell
 * Horseman. The life loss happens even if the destroyed permanent is indestructible or regenerates,
 * but not if Swift End fizzles because its target became illegal.
 */
val MurderousRider = card("Murderous Rider") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Knight"
    oracleText = "Lifelink\nWhen this creature dies, put it on the bottom of its owner's library."
    power = 2
    toughness = 3

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.PutOnBottomOfLibrary(EffectTarget.Self)
    }

    adventure("Swift End") {
        manaCost = "{1}{B}{B}"
        typeLine = "Instant — Adventure"
        oracleText = "Destroy target creature or planeswalker. You lose 2 life. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            val t = target(Targets.CreatureOrPlaneswalker)
            effect = Effects.Destroy(t) then Effects.LoseLife(2, EffectTarget.Controller)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "97"
        artist = "Josh Hass"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e73d8a84-2c0d-423c-89c7-71de0af9e1ac.jpg?1783932635"
    }
}
