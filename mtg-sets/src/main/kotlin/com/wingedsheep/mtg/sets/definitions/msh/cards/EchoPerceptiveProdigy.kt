package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Echo, Perceptive Prodigy — Marvel Super Heroes #51 (uncommon)
 * {2}{U} · Legendary Creature — Human Hero · 1/4
 *
 * Vigilance
 * {1}, {T}: Copy target activated or triggered ability you control from a creature source. You may
 * choose new targets for the copy. (Mana abilities can't be targeted.)
 *
 * Composed entirely from existing primitives plus one new predicate:
 *  - [Targets.ActivatedOrTriggeredAbilityYouControlFrom]`(Creature)` — the existing
 *    "target activated or triggered ability you control" filter (Gogo, Master of Mimicry;
 *    Peter Parker's Camera) narrowed by `CardPredicate.AbilitySourceMatches(Creature)`. The
 *    restriction is on the ability's *source* (CR 113.7), not on the ability object, which has no
 *    characteristics of its own; the source is read with last known information, so a creature's
 *    dies trigger — whose source is already in the graveyard when the trigger is on the stack —
 *    is still "from a creature source" (CR 113.7a). Mana abilities never use the stack, so the
 *    reminder text needs no modelling.
 *  - [Effects.CopyTargetSpellOrAbility] — copies the chosen ability on the stack and reprompts for
 *    new targets per CR 707.10c (the copy has the same source as the original, CR 707.10b).
 *  - `holdPriority` keeps auto-pass from resolving your own ability before you get the chance to
 *    copy it; the enumerator only surfaces the hint when a legal ability is actually on the stack.
 */
val EchoPerceptiveProdigy = card("Echo, Perceptive Prodigy") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Hero"
    power = 1
    toughness = 4
    oracleText = "Vigilance\n" +
        "{1}, {T}: Copy target activated or triggered ability you control from a creature source. " +
        "You may choose new targets for the copy. (Mana abilities can't be targeted.)"

    keywords(Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val ability = target(
            "activated or triggered ability you control from a creature source",
            Targets.ActivatedOrTriggeredAbilityYouControlFrom(GameObjectFilter.Creature)
        )
        effect = Effects.CopyTargetSpellOrAbility(ability)
        holdPriority = true
        description = "{1}, {T}: Copy target activated or triggered ability you control from a " +
            "creature source. You may choose new targets for the copy."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "51"
        artist = "Jurijus Chitrovas"
        flavorText = "\"I've seen the fighting techniques of some of the best. Now their strengths " +
            "are mine.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd9b9be6-0143-467c-8a2a-937ecafe0473.jpg?1783902961"
    }
}
