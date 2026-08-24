package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Lurker
 * {2}{G}
 * Creature — Beast
 * 2/3
 * This creature can't be the target of spells unless it attacked or blocked this turn.
 *
 * **Spells only** — an ability may target it either way, which is what separates this from shroud
 * and why the engine has to know whether the targeting object is a spell or an ability.
 *
 * The "unless" is a `ConditionalStaticAbility` gating a keyword grant, the same shape Goblin Rock
 * Sled uses for its untap clause: the projector re-evaluates the gate continuously, so the Lurker
 * becomes targetable the moment it attacks or blocks and goes back to hiding at end of turn. A flag
 * granted once could not express that.
 *
 * The condition is negated because the flag expresses the *restriction*: it applies while the
 * creature has **not** attacked or blocked.
 */
val Lurker = card("Lurker") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    power = 2
    toughness = 3
    oracleText = "This creature can't be the target of spells unless it attacked or blocked this turn."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(AbilityFlag.CANT_BE_TARGETED_BY_SPELLS.name, GroupFilter.source()),
            condition = Conditions.Not(Conditions.SourceAttackedOrBlockedThisTurn),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "80"
        artist = "Anson Maddocks"
        flavorText = "\"Each night we felt it watching us from the darkness beyond our fire. We " +
            "only had one pack horse left.\"\n—Maeveen O'Donagh, *Memoirs of a Soldier*"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b39eb671-e17e-4c5a-8913-1e3be7faedfb.jpg?1783947931"
    }
}
