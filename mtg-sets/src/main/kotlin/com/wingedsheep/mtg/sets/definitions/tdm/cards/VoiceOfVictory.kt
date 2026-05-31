package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.conditions.IsYourTurn
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Voice of Victory — Tarkir: Dragonstorm #33
 * {1}{W} · Creature — Human Bard · 1/3
 *
 * Mobilize 2 (Whenever this creature attacks, create two tapped and attacking 1/1 red Warrior
 * creature tokens. Sacrifice them at the beginning of the next end step.)
 * Your opponents can't cast spells during your turn.
 *
 * Mobilize is the `mobilize(n)` builder helper. The lock clause is the reusable
 * [PlayersCantCastSpells] static ability — here scoped to `EachOpponent` during your turn
 * (`condition = IsYourTurn`). The engine reads it at cast-legality time, so it blocks every
 * casting zone during this card's controller's turn without any per-zone wiring.
 */
val VoiceOfVictory = card("Voice of Victory") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Bard"
    power = 1
    toughness = 3
    oracleText = "Mobilize 2 (Whenever this creature attacks, create two tapped and attacking 1/1 red Warrior creature tokens. Sacrifice them at the beginning of the next end step.)\n" +
        "Your opponents can't cast spells during your turn."

    mobilize(2)

    staticAbility {
        ability = PlayersCantCastSpells(affected = Player.EachOpponent, condition = IsYourTurn)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "33"
        artist = "Joshua Cairos"
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec3de5f4-bb55-4ab9-995f-f3e0dc22c1bb.jpg?1758215927"
    }
}
