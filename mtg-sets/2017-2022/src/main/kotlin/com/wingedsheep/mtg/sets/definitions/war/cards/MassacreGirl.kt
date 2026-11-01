package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Massacre Girl — War of the Spark #99 (canonical printing)
 * {3}{B}{B} · Legendary Creature — Human Assassin · 4/4
 *
 * Menace
 * When Massacre Girl enters, each other creature gets -1/-1 until end of turn. Whenever a creature
 * dies this turn, each creature other than Massacre Girl gets -1/-1 until end of turn.
 *
 * The "whenever a creature dies this turn" clause is Death Frenzy's shape: a global triggered
 * ability lasting until end of turn, created after the first -1/-1 so it sees the deaths that sweep
 * causes (they happen as state-based actions after the enters trigger resolves). Its source is
 * Massacre Girl, so `AllOtherCreatures` keeps her out of every ripple; being global, it keeps
 * triggering even if she has left the battlefield, and each death it causes triggers it again.
 */
val MassacreGirl = card("Massacre Girl") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Assassin"
    power = 4
    toughness = 4
    oracleText = "Menace\n" +
        "When Massacre Girl enters, each other creature gets -1/-1 until end of turn. Whenever a " +
        "creature dies this turn, each creature other than Massacre Girl gets -1/-1 until end of turn."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Group.modifyStatsForAll(-1, -1, GroupFilter.AllOtherCreatures) then
            Effects.CreateGlobalTriggeredAbility(
                duration = Duration.EndOfTurn,
                ability = TriggeredAbility.create(
                    trigger = Triggers.a(GameObjectFilter.Creature).dies(),
                    effect = Patterns.Group.modifyStatsForAll(-1, -1, GroupFilter.AllOtherCreatures)
                ),
                descriptionOverride = "Whenever a creature dies this turn, each creature other than " +
                    "Massacre Girl gets -1/-1 until end of turn."
            )
        description = "When Massacre Girl enters, each other creature gets -1/-1 until end of turn. " +
            "Whenever a creature dies this turn, each creature other than Massacre Girl gets -1/-1 until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "99"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be8ec9e1-2c8e-496d-9111-4d453b75b578.jpg?1783933441"
    }
}
