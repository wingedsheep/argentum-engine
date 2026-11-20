package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Akroan Conscriptor — Born of the Gods #87 (canonical printing)
 * {4}{R}
 * Creature — Human Shaman
 * 3/2
 * Heroic — Whenever you cast a spell that targets this creature, gain control of another target
 * creature until end of turn. Untap that creature. It gains haste until end of turn.
 *
 * Heroic's `TargetsSource` cast trigger carrying Portent of Betrayal's threaten chain.
 */
val AkroanConscriptor = card("Akroan Conscriptor") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Shaman"
    power = 3
    toughness = 2
    oracleText = "Heroic — Whenever you cast a spell that targets this creature, gain control of another " +
        "target creature until end of turn. Untap that creature. It gains haste until end of turn."

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        val creature = target(TargetFilter.OtherCreature)
        effect = Effects.GainControl(creature, Duration.EndOfTurn) then
            Effects.Untap(creature) then
            Effects.GrantKeyword(Keyword.HASTE, creature)
        description = "Heroic — Whenever you cast a spell that targets this creature, gain control of another " +
            "target creature until end of turn. Untap that creature. It gains haste until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "87"
        artist = "James Ryman"
        flavorText = "\"The time to serve is now.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/e/decb529f-44fd-409d-932f-be9c403f12f6.jpg?1783939549"
        ruling("2014-02-01", "The ability can target any creature except Akroan Conscriptor, including one that's untapped or one you already control.")
        ruling("2014-02-01", "Gaining control of a creature doesn't cause you to gain control of any Auras or Equipment attached to it. Notably, if that creature is enchanted by an Aura with bestow and dies while under your control, the Aura's controller will continue to control the creature that Aura becomes.")
        ruling("2014-02-01", "If you control a legendary creature and gain control of another legendary creature with the same name, you'll choose one to remain on the battlefield and put the other into its owner's graveyard.")
        ruling("2013-09-15", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2013-09-15", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2013-09-15", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
