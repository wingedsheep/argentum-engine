package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Life of Toshiro Umezawa // Memory of Toshiro
 * {1}{B} — Enchantment — Saga // Enchantment Creature — Human Samurai 2/3
 *
 * Front — Life of Toshiro Umezawa:
 *   I, II — Choose one —
 *   • Target creature gets +2/+2 until end of turn.
 *   • Target creature gets -1/-1 until end of turn.
 *   • You gain 2 life.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 *
 * Back — Memory of Toshiro:
 *   {T}, Pay 1 life: Add {B}. Spend this mana only to cast an instant or sorcery spell.
 *
 * Chapters I and II are modal triggered abilities: the mode and its target are chosen as the
 * chapter ability goes on the stack (CR 700.2b), and a targeted mode with no legal creature simply
 * isn't offered.
 */
private val MemoryOfToshiro = card("Memory of Toshiro") {
    manaCost = ""
    colorIndicator = "B"
    colorIdentity = "B"
    typeLine = "Enchantment Creature — Human Samurai"
    oracleText = "{T}, Pay 1 life: Add {B}. Spend this mana only to cast an instant or sorcery spell."
    power = 2
    toughness = 3

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(1))
        effect = Effects.AddMana(Color.BLACK, restriction = ManaRestriction.InstantOrSorceryOnly)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "108"
        artist = "Sidharth Chaturvedi"
        flavorText = "\"Some called him the hero of the Kami War, others a selfish thief. As ever, the truth " +
            "is hidden somewhere in between.\"\n—Reki, founder of the Living Historians"
        imageUri = "https://cards.scryfall.io/normal/back/f/7/f7f4adf7-b871-417b-8904-7f024ca21486.jpg?1783923889"
    }
}

private fun toshiroChapterModes() = ModalEffect.chooseOne(
    mode("Target creature gets +2/+2 until end of turn") {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 2, creature)
    },
    mode("Target creature gets -1/-1 until end of turn") {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(-1, -1, creature)
    },
    mode("You gain 2 life") {
        effect = Effects.GainLife(2)
    },
    countsAsModalSpell = false,
)

private val LifeOfToshiroUmezawaFront = card("Life of Toshiro Umezawa") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I, II — Choose one —\n" +
        "• Target creature gets +2/+2 until end of turn.\n" +
        "• Target creature gets -1/-1 until end of turn.\n" +
        "• You gain 2 life.\n" +
        "III — Exile this Saga, then return it to the battlefield transformed under your control."

    for (chapter in 1..2) {
        sagaChapter(chapter) {
            effect = toshiroChapterModes()
        }
    }

    sagaChapter(3) {
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "108"
        artist = "Sidharth Chaturvedi"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7f4adf7-b871-417b-8904-7f024ca21486.jpg?1783923889"
    }
}

val LifeOfToshiroUmezawa: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = LifeOfToshiroUmezawaFront,
    backFace = MemoryOfToshiro,
)
