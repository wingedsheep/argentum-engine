package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Psychic Rebuttal
 * {1}{U}
 * Instant
 *
 * Counter target instant or sorcery spell that targets you.
 * Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard, you may
 * copy the spell countered this way. You may choose new targets for the copy.
 *
 * - "That targets you" is [CardPredicate.TargetsPlayer] on the stack target, beside the
 *   instant-or-sorcery type test; the spell may have other targets too (ruling).
 * - "The spell countered this way" is the counter's `storeCounteredAs` collection: it holds the
 *   spell only if it was actually countered, so a spell that can't be countered gives no copy
 *   (ruling). The copy is made from the spell as it last existed on the stack, under your control.
 * - Spell mastery is a resolution-time [Conditions.CardsInGraveyardMatchingAtLeast], as on Dark
 *   Petition; Psychic Rebuttal itself is still on the stack then, so it never counts.
 */
val PsychicRebuttal = card("Psychic Rebuttal") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target instant or sorcery spell that targets you.\n" +
        "Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard, " +
        "you may copy the spell countered this way. You may choose new targets for the copy."

    spell {
        target(
            TargetFilter(
                GameObjectFilter(
                    cardPredicates = listOf(
                        CardPredicate.Or(listOf(CardPredicate.IsInstant, CardPredicate.IsSorcery)),
                        CardPredicate.TargetsPlayer(Player.You),
                    )
                ),
                zone = Zone.STACK,
            )
        )
        effect = Effects.Pipeline {
            val countered = runStoringCollection { Effects.CounterSpell(storeCounteredAs = it) }
            run(
                Effects.If(
                    condition = Conditions.All(
                        Conditions.CardsInGraveyardMatchingAtLeast(2, GameObjectFilter.InstantOrSorcery),
                        Conditions.CollectionContainsMatch(countered),
                    ),
                    then = Effects.May(Effects.CopyTargetSpell(countered.asTarget)),
                )
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "67"
        artist = "Todd Lockwood"
        flavorText = "\"I know what you're thinking,\" Jace whispered in the mage's mind. \"It's not going to work.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67a105f8-0c01-4c09-a3bf-8c912b6dc741.jpg?1783938349"
        ruling("2015-06-22", "The target instant or sorcery spell can have other targets as long as one of them is you.")
        ruling("2015-06-22", "If the spell mastery ability applies, you'll create a copy of the instant or sorcery spell only if Psychic Rebuttal counters that spell. If that spell can't be countered by spells or abilities, you won't get a copy.")
        ruling("2015-06-22", "If Psychic Rebuttal creates a copy of the spell, you control the copy. That copy is created on the stack, so it's not \"cast.\" Abilities that trigger when a player casts a spell won't trigger. The copy will then resolve like a normal spell, after players get a chance to cast spells and activate abilities.")
        ruling("2015-06-22", "The copy will have the same targets as the spell it's copying unless you choose new ones. You may change any number of the targets, including all of them or none of them. If, for one of the targets, you can't choose a new legal target, then it remains unchanged (even if the current target is illegal).")
        ruling("2015-06-22", "Check to see if there are two or more instant and/or sorcery cards in your graveyard as the spell resolves to determine whether the spell mastery ability applies. The spell itself won't count because it's still on the stack as you make this check.")
    }
}
