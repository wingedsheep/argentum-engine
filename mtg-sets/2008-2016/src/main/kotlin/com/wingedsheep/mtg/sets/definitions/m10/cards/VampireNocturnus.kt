package com.wingedsheep.mtg.sets.definitions.m10.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.RevealTopOfLibrary
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vampire Nocturnus — Magic 2010 #118 (canonical printing)
 * {1}{B}{B}{B} · Creature — Vampire · 3/3
 *
 * Play with the top card of your library revealed.
 * As long as the top card of your library is black, this creature and other Vampire
 * creatures you control get +2/+1 and have flying.
 *
 * "This creature" and "other Vampire creatures you control" are separate groups: per the
 * 2013-07-01 ruling Vampire Nocturnus buffs itself even if it's somehow not a Vampire, so the
 * self grant uses [Filters.Self] rather than a Vampire filter that happens to include it.
 */
private val topCardIsBlack =
    Conditions.EntityMatches(EffectTarget.LibraryTop(), GameObjectFilter.Any.withColor(Color.BLACK))

private val otherVampiresYouControl =
    GroupFilter(GameObjectFilter.Creature.withSubtype("Vampire").youControl(), excludeSelf = true)

val VampireNocturnus = card("Vampire Nocturnus") {
    manaCost = "{1}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    power = 3
    toughness = 3
    oracleText = "Play with the top card of your library revealed.\n" +
        "As long as the top card of your library is black, this creature and other Vampire creatures you control get +2/+1 and have flying."

    staticAbility { ability = RevealTopOfLibrary }

    staticAbility {
        condition = topCardIsBlack
        ability = ModifyStats(powerBonus = 2, toughnessBonus = 1, filter = Filters.Self)
    }
    staticAbility {
        condition = topCardIsBlack
        ability = GrantKeyword(Keyword.FLYING, Filters.Self)
    }
    staticAbility {
        condition = topCardIsBlack
        ability = ModifyStats(powerBonus = 2, toughnessBonus = 1, filter = otherVampiresYouControl)
    }
    staticAbility {
        condition = topCardIsBlack
        ability = GrantKeyword(Keyword.FLYING, otherVampiresYouControl)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "118"
        artist = "Raymond Swanland"
        flavorText = "\"Your life will set with the sun.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9df4f1ea-dbaa-456c-884c-97f03b64fa17.jpg?1783942378"
        ruling("2013-07-01", "If the top card of your library is black, Vampire Nocturnus will give itself +2/+1 and flying even if it's somehow not a Vampire.")
        ruling("2013-04-15", "The top card of your library isn't in your hand, so you can't suspend it, cycle it, discard it, or activate any of its activated abilities.")
        ruling("2013-04-15", "If the top card of your library changes while you're casting a spell, playing a land, or activating an ability, the new top card won't be revealed until you finish doing so.")
        ruling("2013-04-15", "When playing with the top card of your library revealed, if an effect tells you to draw several cards, reveal each one before you draw it.")
        ruling("2009-10-01", "To work as an evasion ability, an attacking creature must already have flying when the declare blockers step begins. Once a creature has become blocked, giving it flying won't change that.")
    }
}
