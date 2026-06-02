package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherUntilMatchEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.RevealCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.dsl.Effects

/**
 * Sanar, Innovative First-Year
 * {2}{U/R}{U/R}
 * Legendary Creature — Goblin Sorcerer
 * 2/4
 *
 * Vivid — At the beginning of your first main phase, reveal cards from the top of your
 * library until you reveal X nonland cards, where X is the number of colors among permanents
 * you control. For each of those colors, you may exile a card of that color from among the
 * revealed cards. Then shuffle. You may cast the exiled cards this turn.
 *
 * The Vivid mechanic uses [DynamicAmounts.colorsAmongPermanents] to compute X via the projected
 * battlefield (so recolor effects apply). The "for each of those colors, you may exile a card
 * of that color" clause is enforced by a [SelectionMode.ChooseUpTo] cap of X plus a
 * [SelectionRestriction.OnePerColor] with `matchControllerPermanentColors = true`: revealed
 * nonland cards whose colours don't intersect the controller's permanent colours are filtered
 * out, and at most one card per controller-colour can be exiled (so two cards sharing a colour
 * can't both be picked even if both intersect the controller's colours).
 */
val SanarInnovativeFirstYear = card("Sanar, Innovative First-Year") {
    manaCost = "{2}{U/R}{U/R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Goblin Sorcerer"
    power = 2
    toughness = 4
    oracleText = "Vivid — At the beginning of your first main phase, reveal cards from the top of " +
        "your library until you reveal X nonland cards, where X is the number of colors among " +
        "permanents you control. For each of those colors, you may exile a card of that color " +
        "from among the revealed cards. Then shuffle. You may cast the exiled cards this turn."

    keywords(Keyword.VIVID)

    val colorCount = DynamicAmounts.colorsAmongPermanents()

    triggeredAbility {
        trigger = Triggers.FirstMainPhase
        effect = Effects.Composite(listOf(
            // Walk the library until X nonland cards have been revealed.
            GatherUntilMatchEffect(
                filter = GameObjectFilter.Nonland,
                storeMatch = "nonlandCards",
                storeRevealed = "allRevealed",
                count = colorCount
            ),
            RevealCollectionEffect(from = "allRevealed"),
            // The caster picks one card per colour (up to X) of the revealed nonlands to exile.
            SelectFromCollectionEffect(
                from = "nonlandCards",
                selection = SelectionMode.ChooseUpTo(colorCount),
                storeSelected = "toExile",
                selectedLabel = "Exile (you may cast this turn)",
                alwaysPrompt = true,
                restrictions = listOf(SelectionRestriction.OnePerColor(matchControllerPermanentColors = true))
            ),
            MoveCollectionEffect(
                from = "toExile",
                destination = CardDestination.ToZone(Zone.EXILE)
            ),
            // Everything revealed minus the exiled cards goes back to the library, shuffled.
            FilterCollectionEffect(
                from = "allRevealed",
                filter = CollectionFilter.ExcludeOtherCollection("toExile"),
                storeMatching = "toLibrary"
            ),
            MoveCollectionEffect(
                from = "toLibrary",
                destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Shuffled)
            ),
            // You may cast the exiled cards this turn (paying their normal costs).
            GrantMayPlayFromExileEffect(from = "toExile")
        ))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "241"
        artist = "Steven Belledin"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11215561-bbcd-4564-a2e4-a1d77d177a1d.jpg?1767687679"
        ruling("2025-11-17", "The value of X is calculated only once, as Sanar's ability resolves.")
        ruling(
            "2025-11-17",
            "You pay all costs and follow all timing rules for cards cast this way. For example, " +
                "if an exiled card is a sorcery card, you may cast it only during your main phase " +
                "while the stack is empty."
        )
    }
}
