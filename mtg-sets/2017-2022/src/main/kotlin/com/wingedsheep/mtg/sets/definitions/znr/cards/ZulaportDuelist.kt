package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Zulaport Duelist — Zendikar Rising #88
 * {U} · Creature — Human Rogue · 1/1
 *
 * Flash
 * When this creature enters, up to one target creature gets -2/-0 until end of turn. Its
 * controller mills two cards.
 *
 * The mill runs through [Effects.ForEachPlayer] over [Player.ControllerOf] the target rather than
 * a bare `mill(2, PlayerRef(ControllerOf))`: the mill's player resolution falls back to the
 * ability's controller when no player resolves, so with zero targets chosen a bare mill would
 * wrongly mill *you*. `ForEachPlayer` over an unresolved `ControllerOf` iterates nobody, so
 * declining the target mills no one (same shape as White Orchid Phantom).
 */
val ZulaportDuelist = card("Zulaport Duelist") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Rogue"
    oracleText = "Flash\n" +
        "When this creature enters, up to one target creature gets -2/-0 until end of turn. Its controller " +
        "mills two cards. (They put the top two cards of their library into their graveyard.)"
    power = 1
    toughness = 1

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.ModifyStats(-2, 0, creature) then
            Effects.ForEachPlayer(
                Player.ControllerOf("target creature"),
                Patterns.Library.mill(2),
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "88"
        artist = "Bryan Sola"
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b4c7268-60c6-4038-8ce9-d9d51bed4577.jpg?1783929384"
        ruling(
            "2020-09-25",
            "If the target creature is an illegal target by the time Zulaport Duelist's ability tries to " +
                "resolve, the ability doesn't resolve. No player mills two cards.",
        )
    }
}
