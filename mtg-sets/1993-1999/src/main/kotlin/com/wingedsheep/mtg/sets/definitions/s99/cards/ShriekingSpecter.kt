package com.wingedsheep.mtg.sets.definitions.s99.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val ShriekingSpecter = card("Shrieking Specter") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Specter"
    oracleText = "Flying\nWhenever this creature attacks, defending player discards a card."
    power = 2
    toughness = 2
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Discard(1, EffectTarget.PlayerRef(Player.DefendingPlayer))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "89"
        artist = "rk post"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81d3b0fd-a81e-4665-bba6-b6d085693abf.jpg?1783946031"
    }
}
