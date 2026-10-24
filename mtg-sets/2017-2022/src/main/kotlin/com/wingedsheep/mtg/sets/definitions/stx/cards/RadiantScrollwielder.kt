package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToOwnSpells
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Radiant Scrollwielder — Strixhaven: School of Mages #221 (canonical printing)
 * {2}{R}{W} · Creature — Dwarf Cleric · 2/4
 *
 * Instant and sorcery spells you control have lifelink.
 * At the beginning of your upkeep, exile an instant or sorcery card at random from your
 * graveyard. You may cast it this turn. If a spell cast this way would be put into your
 * graveyard, exile it instead.
 *
 * The lifelink clause is Heartflame Duelist's [GrantKeywordToOwnSpells] over
 * [GameObjectFilter.InstantOrSorcery]. The upkeep trigger is Tersa Lightshatter's random-exile
 * pipeline narrowed to instant and sorcery cards, then Nita, Forum Conciliator's this-turn cast
 * permission with the exile-instead rider. The card's mana cost is still paid (ruling).
 */
val RadiantScrollwielder = card("Radiant Scrollwielder") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Dwarf Cleric"
    power = 2
    toughness = 4
    oracleText = "Instant and sorcery spells you control have lifelink.\n" +
        "At the beginning of your upkeep, exile an instant or sorcery card at random from your " +
        "graveyard. You may cast it this turn. If a spell cast this way would be put into your " +
        "graveyard, exile it instead."

    staticAbility {
        ability = GrantKeywordToOwnSpells(
            keyword = Keyword.LIFELINK,
            spellFilter = GameObjectFilter.InstantOrSorcery
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Pipeline {
            val spells = gather(
                CardSource.FromZone(Zone.GRAVEYARD, Player.You, filter = GameObjectFilter.InstantOrSorcery)
            )
            val exiledAtRandom = chooseRandom(1, from = spells)
            exile(exiledAtRandom)
            run(Effects.GrantMayPlayFromExile(
                from = exiledAtRandom,
                expiry = MayPlayExpiry.EndOfTurn,
                insteadOfGraveyard = AfterResolveDestination.EXILE,
            ))
        }
        description = "At the beginning of your upkeep, exile an instant or sorcery card at random " +
            "from your graveyard. You may cast it this turn. If a spell cast this way would be put " +
            "into your graveyard, exile it instead."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "221"
        artist = "Campbell White"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c8152ab0-c6fc-4568-b837-4e84129e7b57.jpg?1783927297"
        ruling("2021-04-16", "You still pay the mana cost when casting the exiled card, and you must follow all timing rules for it.")
        ruling("2021-04-16", "If an instant or sorcery spell you control with lifelink deals damage to you, you gain and lose that much life simultaneously. In this case, your life total doesn't change. (You may gain life if the spell also deals damage to other permanents and/or players, however.)")
        ruling("2021-04-16", "Some instant and sorcery spells instruct other things to deal damage rather than deal damage themselves. In these cases, you won't gain life for that damage (unless the other things also have lifelink). Notably, a spell that instructs a creature you control to fight won't cause you to gain life.")
    }
}
