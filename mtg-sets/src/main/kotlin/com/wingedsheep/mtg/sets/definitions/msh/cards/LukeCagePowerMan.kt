package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Luke Cage, Power Man — Marvel Super Heroes #20
 * {3}{W} · Legendary Creature — Human Hero · 2/5
 *
 * Unbreakable Skin — Whenever Luke Cage attacks alone, he gets +2/+0 and gains indestructible
 * until end of turn.
 *
 * "Unbreakable Skin" is an ability word (CR 207.2c) — flavor only, no rules meaning, so it lives
 * in the oracle text / description rather than as a keyword. The trigger is the SELF-bound
 * "attacks alone" shape (Rogue Kavu): [Triggers.attacks] with [AttackPredicate.Alone] and the
 * default SELF binding, buffing [EffectTarget.Self].
 */
val LukeCagePowerMan = card("Luke Cage, Power Man") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Hero"
    power = 2
    toughness = 5
    oracleText = "Unbreakable Skin — Whenever Luke Cage attacks alone, he gets +2/+0 and gains " +
        "indestructible until end of turn. (Damage and effects that say \"destroy\" don't destroy him.)"

    triggeredAbility {
        trigger = Triggers.attacks(requires = setOf(AttackPredicate.Alone))
        effect = Effects.Composite(
            Effects.ModifyStats(2, 0, EffectTarget.Self),
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self, Duration.EndOfTurn),
        )
        description = "Unbreakable Skin — Whenever Luke Cage attacks alone, he gets +2/+0 and " +
            "gains indestructible until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "20"
        artist = "Aniekan Udofia"
        flavorText = "\"People call me with their problems. They feel the cops don't care, the " +
            "Avengers got bigger issues, and Spidey's number isn't listed. But they have my " +
            "number, so they call me.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b72c9e9-1cb2-4374-873a-53af293d86d0.jpg?1783902972"
    }
}
