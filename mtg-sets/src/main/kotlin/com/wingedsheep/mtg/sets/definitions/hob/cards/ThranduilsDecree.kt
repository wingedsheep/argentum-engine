package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect

/**
 * Thranduil's Decree — The Hobbit #56
 * {4}{U}{U} · Instant · Uncommon
 *
 * Counter target spell. If a permanent spell is countered this way, exile it instead of putting it
 * into its owner's graveyard. You may cast that card without paying its mana cost for as long as it
 * remains exiled.
 *
 * Modeling notes:
 *  - The exile rider is conditional on the countered spell being a *permanent* spell, so the two
 *    counter destinations are a [ConditionalEffect] rather than a single
 *    `CounterSpellToExile`: an instant or sorcery countered by the Decree goes to its owner's
 *    graveyard as usual and is never castable from exile. [Conditions.TargetMatchesFilter] is
 *    evaluated at resolution, while the countered spell is still on the stack, and the predicate
 *    evaluator reads the stack object's own card types — the same path Dissipate's sibling shapes
 *    take through `ChosenTarget.Spell`.
 *  - The free-cast permission is `CounterDestination.Exile(grantFreeCast = true)` (Kheru
 *    Spellsnatcher's rider): the Decree's controller — not the countered spell's — may cast the
 *    exiled card without paying its mana cost, and the permission lasts exactly as long as the card
 *    stays in exile, matching the printed "for as long as it remains exiled".
 *  - A spell that can't be countered is unaffected by either branch; the counter simply does
 *    nothing, and no exile happens (CR 701.5a).
 */
val ThranduilsDecree = card("Thranduil's Decree") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell. If a permanent spell is countered this way, exile it " +
        "instead of putting it into its owner's graveyard. You may cast that card without paying " +
        "its mana cost for as long as it remains exiled."

    spell {
        target = Targets.Spell
        effect = ConditionalEffect(
            condition = Conditions.TargetMatchesFilter(GameObjectFilter.Permanent),
            effect = Effects.CounterSpellToExile(grantFreeCast = true),
            elseEffect = Effects.CounterSpell()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "56"
        artist = "Javier Charro"
        flavorText = "\"I have a right to know what brings you here, and if you will not tell me " +
            "now, I will keep you all in prison until you have learned sense and manners!\""
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4ded4c1-0e3e-47c5-8fdc-e7c187f68b12.jpg?1784760181"
    }
}
