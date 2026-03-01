package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Xantid Swarm
 * {G}
 * Creature — Insect
 * 0/1
 * Flying
 * Whenever Xantid Swarm attacks, defending player can't cast spells this turn.
 */
val XantidSwarm = card("Xantid Swarm") {
    manaCost = "{G}"
    typeLine = "Creature — Insect"
    oracleText = "Flying\nWhenever Xantid Swarm attacks, defending player can't cast spells this turn."
    power = 0
    toughness = 1

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.CantCastSpells(EffectTarget.PlayerRef(Player.Opponent))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "135"
        artist = "David Martin"
        flavorText = "When they land on you, all you can think about is tearing them off."
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a87911a-3931-46aa-9348-2728c4b73b96.jpg?1562530173"
        ruling("6/8/2016", "The defending player may cast spells before Xantid Swarm's triggered ability resolves.")
        ruling("6/8/2016", "In a multiplayer game, only the player Xantid Swarm attacked is affected. Other defending players may cast spells.")
    }
}
