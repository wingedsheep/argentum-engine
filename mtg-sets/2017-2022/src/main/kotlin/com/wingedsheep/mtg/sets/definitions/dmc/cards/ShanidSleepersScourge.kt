package com.wingedsheep.mtg.sets.definitions.dmc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shanid, Sleepers' Scourge
 * {1}{R}{W}{B}
 * Legendary Creature — Human Knight
 * 2/4
 * Menace
 * Other legendary creatures you control have menace.
 * Whenever you play a legendary land or cast a legendary spell, you draw a card and you lose 1 life.
 *
 * The last ability is split into one triggered ability per event, as Cemetery Protector does.
 * The land half narrows "you play a land" to legendary lands with a trigger-time restriction on
 * the triggering entity (CR 603.2, not an intervening "if"), so the land leaving before resolution
 * doesn't stop the draw; lands put onto the battlefield without being played don't trigger it
 * (ruling).
 */
private val shanidPayoff =
    Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)

val ShanidSleepersScourge = card("Shanid, Sleepers' Scourge") {
    manaCost = "{1}{R}{W}{B}"
    colorIdentity = "RWB"
    typeLine = "Legendary Creature — Human Knight"
    power = 2
    toughness = 4
    oracleText = "Menace\n" +
        "Other legendary creatures you control have menace.\n" +
        "Whenever you play a legendary land or cast a legendary spell, you draw a card and you lose 1 life."

    keywords(Keyword.MENACE)

    staticAbility {
        ability = GrantKeyword(
            Keyword.MENACE,
            GroupFilter(GameObjectFilter.Creature.legendary().youControl(), excludeSelf = true),
        )
    }

    triggeredAbility {
        trigger = Triggers.you.playsLand()
        triggerRestriction = Conditions.EntityMatches(
            EffectTarget.TriggeringEntity,
            GameObjectFilter.Any.legendary(),
        )
        effect = shanidPayoff
        description = "Whenever you play a legendary land, you draw a card and you lose 1 life."
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.legendary())
        effect = shanidPayoff
        description = "Whenever you cast a legendary spell, you draw a card and you lose 1 life."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "4"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b0fb859-055d-4aa1-814c-0894cd2ccd4a.jpg?1783921477"
        ruling("2022-09-09", "If casting a legendary spell causes Shanid's last ability to trigger, that ability goes on the stack above the spell that caused it to trigger and resolves first. It resolves even if that spell is countered or otherwise left the stack.")
        ruling("2022-09-09", "A legendary land entering the battlefield without being played does not cause Shanid's last ability to trigger.")
    }
}
