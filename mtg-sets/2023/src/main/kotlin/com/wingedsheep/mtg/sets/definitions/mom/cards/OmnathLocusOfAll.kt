package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConvertEmptyingMana
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Omnath, Locus of All — March of the Machine #249
 * {W}{U}{B/P}{R}{G} · Legendary Creature — Phyrexian Elemental · 4/4 · Rare
 *
 * If you would lose unspent mana, that mana becomes black instead.
 * At the beginning of your first main phase, look at the top card of your library. You may reveal
 * that card if it has three or more colored mana symbols in its mana cost. If you do, add three mana
 * in any combination of its colors and put it into your hand. If you don't reveal it, put it into
 * your hand.
 *
 * - The first ability is [ConvertEmptyingMana] (black) — Ozai's static with a different colour.
 *   Restricted mana keeps its restriction when it turns black (ruling).
 * - The look is a gather; "you may reveal it if …" is a `chooseUpTo(1)` filtered to
 *   `coloredManaSymbolsAtLeast(all colours, min = 3)`, so a card with fewer pips can't be picked.
 * - "Three mana in any combination of its colors" is three single units of
 *   [ManaColorSet.ColorsOf] the revealed card, each choosing its own colour. The mana is added while
 *   the card is still on top of the library; the card goes to hand either way.
 * - The triggered ability isn't a mana ability (ruling) — it uses the stack.
 */
val OmnathLocusOfAll = card("Omnath, Locus of All") {
    manaCost = "{W}{U}{B/P}{R}{G}"
    colorIdentity = "WUBRG"
    typeLine = "Legendary Creature — Phyrexian Elemental"
    power = 4
    toughness = 4
    oracleText = "If you would lose unspent mana, that mana becomes black instead.\n" +
        "At the beginning of your first main phase, look at the top card of your library. You may " +
        "reveal that card if it has three or more colored mana symbols in its mana cost. If you do, " +
        "add three mana in any combination of its colors and put it into your hand. If you don't " +
        "reveal it, put it into your hand."

    staticAbility {
        ability = ConvertEmptyingMana(Color.BLACK)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.PRECOMBAT_MAIN)
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(1))
            val revealed = chooseUpTo(
                1,
                from = looked,
                filter = GameObjectFilter.Any.coloredManaSymbolsAtLeast(*Color.entries.toTypedArray(), min = 3),
                prompt = "You may reveal the top card of your library",
                selectedLabel = "Reveal",
                showAllCards = true
            )
            reveal(revealed, revealToSelf = false)
            run(Effects.If(
                condition = whenMatches(revealed),
                then = Effects.Repeat(
                    DynamicAmounts.fixed(3),
                    Effects.AddManaOfChoice(ManaColorSet.ColorsOf(revealed.asTarget(0)))
                )
            ))
            toHand(looked)
        }
        description = "At the beginning of your first main phase, look at the top card of your " +
            "library. You may reveal that card if it has three or more colored mana symbols in its " +
            "mana cost. If you do, add three mana in any combination of its colors and put it into " +
            "your hand. If you don't reveal it, put it into your hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "249"
        artist = "Bryan Sola"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33d94ecf-758b-4f68-a7be-6bf3ff1047f4.jpg?1783916940"
        ruling(
            "2023-04-14",
            "As long as Omnath, Locus of All remains under your control, you'll retain unspent mana as " +
                "steps and phases end, although that mana will become black. Once Omnath leaves your " +
                "control, you'll have until the end of the current step or phase to spend the mana before it is lost."
        )
        ruling(
            "2023-04-14",
            "If unspent mana you have has any restrictions or riders associated with it, those " +
                "restrictions or riders remain associated with that mana when it becomes black."
        )
        ruling(
            "2023-04-14",
            "Omnath's last ability isn't a mana ability even though it can cause you to add mana. It " +
                "uses the stack and can be responded to."
        )
    }
}
