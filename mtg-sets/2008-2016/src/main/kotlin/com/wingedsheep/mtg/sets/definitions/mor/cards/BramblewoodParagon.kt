package com.wingedsheep.mtg.sets.definitions.mor.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Bramblewood Paragon — Morningtide #115
 * {1}{G} · Creature — Elf Warrior · 2/2
 *
 * Each other Warrior creature you control enters with an additional +1/+1 counter on it.
 * Each creature you control with a +1/+1 counter on it has trample.
 *
 * The first line is Metallic Mimic's `otherOnly` [EntersWithCounters] replacement narrowed to
 * Warriors — `otherOnly` keeps the Paragon (itself a Warrior) from countering its own entry, and a
 * Warrior entering simultaneously with it gets nothing (ruling). Two Paragons stack. The second is
 * Duskshell Crawler's [GrantKeyword] over creatures you control with a +1/+1 counter.
 */
val BramblewoodParagon = card("Bramblewood Paragon") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Warrior"
    power = 2
    toughness = 2
    oracleText = "Each other Warrior creature you control enters with an additional +1/+1 counter on it.\n" +
        "Each creature you control with a +1/+1 counter on it has trample."

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 1,
            otherOnly = true,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.withSubtype(Subtype.WARRIOR).youControl(),
                to = Zone.BATTLEFIELD,
            ),
        )
    )

    staticAbility {
        ability = GrantKeyword(
            Keyword.TRAMPLE,
            filter = GroupFilter(
                GameObjectFilter.Creature.youControl().withCounter(CounterType.PLUS_ONE_PLUS_ONE),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "115"
        artist = "Jim Murray"
        flavorText = "Those who seek to escape her blades succeed only in dying on their stomachs."
        imageUri = "https://cards.scryfall.io/normal/front/3/9/3910f5b2-17da-41e4-bf40-1c40b513fa12.jpg?1783942781"
        ruling(
            "2008-04-01",
            "If a Warrior would normally enter with a certain number of +1/+1 counters on it, it " +
                "enters with that many +1/+1 counters plus one on it instead. If a Warrior would " +
                "normally enter with no +1/+1 counters on it, it enters with one +1/+1 counter on it instead."
        )
        ruling(
            "2008-04-01",
            "The creature gets the counter if it would enter under your control. It doesn't matter " +
                "who owns the creature or what zone it enters from (such as your opponent's " +
                "graveyard, for example)."
        )
        ruling(
            "2008-04-01",
            "The effects from more than one Bramblewood Paragon are cumulative. That is, if you " +
                "have more than one Bramblewood Paragon on the battlefield, Warrior creatures you " +
                "control enter with that many additional +1/+1 counters on them."
        )
        ruling(
            "2008-04-01",
            "If Bramblewood Paragon enters at the same time as another Warrior (due to Living End, " +
                "for example), that creature doesn't get a +1/+1 counter."
        )
    }
}
