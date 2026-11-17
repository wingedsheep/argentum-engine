package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate

/**
 * Ruthless Instincts
 * {2}{G}
 * Instant
 * Choose one —
 * • Target nonattacking creature gains reach and deathtouch until end of turn. Untap it.
 * • Target attacking creature gets +2/+2 and gains trample until end of turn.
 *
 * "Nonattacking" is the negated [StatePredicate.IsAttacking] — there is no `notAttacking()`
 * builder, so the predicate is spelled out (as on Spider-Man, To the Rescue).
 */
val RuthlessInstincts = card("Ruthless Instincts") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Target nonattacking creature gains reach and deathtouch until end of turn. Untap it.\n" +
        "• Target attacking creature gets +2/+2 and gains trample until end of turn."

    spell {
        effect = ModalEffect.chooseOne(
            mode("Target nonattacking creature gains reach and deathtouch until end of turn. Untap it") {
                val creature = target(
                    TargetFilter.Creature.withStatePredicate(StatePredicate.Not(StatePredicate.IsAttacking))
                )
                effect = Effects.GrantKeyword(Keyword.REACH, creature) then
                    Effects.GrantKeyword(Keyword.DEATHTOUCH, creature) then
                    Effects.Untap(creature)
            },
            mode("Target attacking creature gets +2/+2 and gains trample until end of turn") {
                val creature = target(TargetFilter.AttackingCreature)
                effect = Effects.ModifyStats(2, 2, creature) then
                    Effects.GrantKeyword(Keyword.TRAMPLE, creature)
            },
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "136"
        artist = "Lius Lasahido"
        flavorText = "A single battle affects countless others."
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2c8bfde-c4c5-40de-83de-ff236999d79b.jpg?1783938678"
        ruling(
            "2014-11-24",
            "If you choose the first mode, the target creature must not be attacking. However, it can " +
                "attack later in the turn."
        )
    }
}
