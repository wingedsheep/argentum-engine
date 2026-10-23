package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Ecological Appreciation — Strixhaven: School of Mages #128 (canonical printing)
 * {X}{2}{G} · Sorcery
 *
 * Search your library and graveyard for up to four creature cards with different names that each
 * have mana value X or less and reveal them. An opponent chooses two of those cards. Shuffle the
 * chosen cards into your library and put the rest onto the battlefield. Exile Ecological
 * Appreciation.
 *
 * Modelling notes:
 * - Gifts Ungiven's shape over two zones: one gather across library and graveyard (Vision
 *   Quest's [CardSource.FromMultipleZones], flagged as a search), an up-to-four pick held to
 *   different names by [SelectionRestriction.OnePerCardName], a public reveal, then an opponent's
 *   exactly-two split ([Chooser.Opponent] — with several opponents, the caster picks which one).
 * - The split clamps to what was found, so with one or two cards found the opponent takes them
 *   all and nothing enters the battlefield (ruling); with three, one enters.
 * - The chosen cards are shuffled into the library and the library is shuffled after, which also
 *   finishes the search; the rest enter the battlefield under your control.
 * - "Exile Ecological Appreciation" is `selfExile()` — the resolved sorcery goes to exile.
 */
val EcologicalAppreciation = card("Ecological Appreciation") {
    manaCost = "{X}{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library and graveyard for up to four creature cards with different " +
        "names that each have mana value X or less and reveal them. An opponent chooses two of " +
        "those cards. Shuffle the chosen cards into your library and put the rest onto the " +
        "battlefield. Exile Ecological Appreciation."

    spell {
        selfExile()
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromMultipleZones(
                    zones = listOf(Zone.LIBRARY, Zone.GRAVEYARD),
                    player = Player.You,
                    filter = GameObjectFilter.Creature.manaValueAtMostDynamic(DynamicAmounts.xValue())
                ),
                search = true
            )
            val found = chooseUpTo(
                4,
                from = searchable,
                restrictions = listOf(SelectionRestriction.OnePerCardName),
                prompt = "Search your library and graveyard for up to four creature cards with " +
                    "different names and mana value X or less"
            )
            reveal(found)
            val (toLibrary, toBattlefield) = chooseExactlySplit(
                2,
                from = found,
                chooser = Chooser.Opponent,
                selectedLabel = "Shuffle into library",
                remainderLabel = "Onto the battlefield",
                prompt = "Choose two of the revealed cards. Those are shuffled into their owner's " +
                    "library; the rest are put onto the battlefield."
            )
            move(toLibrary, CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Shuffled))
            move(toBattlefield, CardDestination.ToZone(Zone.BATTLEFIELD))
            run(Effects.ShuffleLibrary())
            // CR 701.23 — the search happened whether or not anything was found.
            run(EmitLibrarySearchedEventEffect)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "128"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/115f3d72-1aaf-4237-91b9-389256e5e5c8.jpg?1783927344"
        ruling("2021-04-16", "If you find and reveal three cards, two of them will still be shuffled into your library. If you find and reveal one or two cards, no creatures will be put onto the battlefield.")
    }
}
