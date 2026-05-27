package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Erebor Flamesmith
 * {1}{R}
 * Creature — Dwarf Artificer
 * 2/1
 *
 * Whenever you cast an instant or sorcery spell, this creature deals 1 damage to each opponent.
 */
val EreborFlamesmith = card("Erebor Flamesmith") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dwarf Artificer"
    power = 2
    toughness = 1
    oracleText = "Whenever you cast an instant or sorcery spell, this creature deals 1 damage to each opponent."

    triggeredAbility {
        trigger = Triggers.YouCastInstantOrSorcery
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "122"
        artist = "L J Koh"
        flavorText = "\"We make good armor and keen swords, but we cannot again make mail or blade to match those that our fathers made before the dragon came.\"\n—Glóin"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/552730c5-e3a6-468f-91b2-82c272dda400.jpg?1686968881"
    }
}
