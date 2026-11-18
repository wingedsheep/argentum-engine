package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Akroan Line Breaker — Journey into Nyx #88 (canonical printing)
 * {2}{R}
 * Creature — Human Warrior
 * 2/1
 * Heroic — Whenever you cast a spell that targets this creature, this creature gets +2/+0 and gains
 * intimidate until end of turn.
 */
val AkroanLineBreaker = card("Akroan Line Breaker") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 1
    oracleText = "Heroic — Whenever you cast a spell that targets this creature, this creature gets +2/+0 and gains intimidate until end of turn. (It can't be blocked except by artifact creatures and/or creatures that share a color with it.)"

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.INTIMIDATE, EffectTarget.Self)
        description = "Heroic — Whenever you cast a spell that targets this creature, this creature gets +2/+0 and gains intimidate until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "88"
        artist = "Marco Nelor"
        flavorText = "The enemies' shields are the first to shatter, and their battle line is never far behind."
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a5114db5-b545-43dc-85a3-c0e68ab22a95.jpg?1783939428"
        ruling("2014-04-26", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2014-04-26", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2014-04-26", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
