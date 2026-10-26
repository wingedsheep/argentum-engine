package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Silumgar, the Drifting Death
 * {4}{U}{B}
 * Legendary Creature — Dragon
 * 3/7
 * Flying, hexproof
 * Whenever a Dragon you control attacks, creatures defending player controls get -1/-1 until end
 * of turn.
 *
 * One trigger per attacking Dragon (ANY binding, Silumgar included). "Defending player" is that
 * Dragon's defender (CR 508.5), so in multiplayer each Dragon shrinks only the creatures of the
 * player it attacks — the referenced-player filter, not `defendingPlayerControls()`, which matches
 * every defending player in the combat. The affected set is locked in at resolution (CR 611.2c).
 */
val SilumgarTheDriftingDeath = card("Silumgar, the Drifting Death") {
    manaCost = "{4}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Dragon"
    power = 3
    toughness = 7
    oracleText = "Flying, hexproof\n" +
        "Whenever a Dragon you control attacks, creatures defending player controls get -1/-1 until end of turn."

    keywords(Keyword.FLYING, Keyword.HEXPROOF)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withSubtype("Dragon").youControl()).attacks()
        effect = Patterns.Group.modifyStatsForAll(
            -1, -1,
            GroupFilter(
                GameObjectFilter.Creature.targetPlayerControls(EffectTarget.PlayerRef(Player.DefendingPlayer))
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "157"
        artist = "Steven Belledin"
        flavorText = "\"No machinations, no puppet strings, no plots. Just pure, sweeping death.\"\n—Tasigur, the Golden Fang"
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4c9108e7-6c39-4587-aa3a-0ec27f9761b2.jpg?1783938673"
    }
}
