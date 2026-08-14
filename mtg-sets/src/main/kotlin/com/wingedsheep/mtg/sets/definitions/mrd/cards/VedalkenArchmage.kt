package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Vedalken Archmage — Mirrodin #55
 * {2}{U}{U} · Creature — Vedalken Wizard · 0/2
 *
 * Whenever you cast an artifact spell, draw a card.
 *
 * [Triggers.youCastSpell] over [GameObjectFilter.Artifact] — a cast trigger, so it fires as the
 * spell goes on the stack and the draw resolves *above* it. It is not once per turn and has no
 * "you may": every artifact spell you cast draws, including one that is later countered or that
 * never resolves.
 */
val VedalkenArchmage = card("Vedalken Archmage") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Vedalken Wizard"
    power = 0
    toughness = 2
    oracleText = "Whenever you cast an artifact spell, draw a card."

    triggeredAbility {
        trigger = Triggers.youCastSpell(spellFilter = GameObjectFilter.Artifact)
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "55"
        artist = "Kev Walker"
        flavorText = "\"The Knowledge Pool knows. Memnarch understands.\"\n—Janus, speaker of the synod"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b38da97-5141-4de6-bd7f-3fcbf46cfd96.jpg?1783944550"
    }
}
