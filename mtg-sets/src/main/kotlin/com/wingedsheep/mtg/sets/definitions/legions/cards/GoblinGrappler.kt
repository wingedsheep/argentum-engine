package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Goblin Grappler
 * {R}
 * Creature — Goblin
 * 1/1
 * Provoke (Whenever this creature attacks, you may have target creature defending player
 * controls untap and block it if able.)
 */
val GoblinGrappler = card("Goblin Grappler") {
    manaCost = "{R}"
    typeLine = "Creature — Goblin"
    power = 1
    toughness = 1
    oracleText = "Provoke (Whenever this creature attacks, you may have target creature defending player controls untap and block it if able.)"

    keywords(Keyword.PROVOKE)

    triggeredAbility {
        trigger = Triggers.Attacks
        optional = true
        target = Targets.CreatureOpponentControls
        effect = Effects.Provoke(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "100"
        artist = "Christopher Moeller"
        flavorText = "Daru soldiers learned it's better to have a clean death from a sharp blade than to tangle with a goblin's rusted chains."
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c948872-295c-41b9-8094-db7db7578b0d.jpg?1562913803"
    }
}
