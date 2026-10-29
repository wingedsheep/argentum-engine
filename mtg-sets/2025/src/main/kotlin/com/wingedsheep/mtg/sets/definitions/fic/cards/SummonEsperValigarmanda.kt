package com.wingedsheep.mtg.sets.definitions.fic.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Summon: Esper Valigarmanda — Final Fantasy Commander #60
 * {3}{R} · Enchantment Creature — Saga Drake · 3/3
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)
 * I — Exile an instant or sorcery card from each graveyard.
 * II, III, IV — Add {R} for each lore counter on this Saga. You may cast an instant or sorcery card
 *     exiled with this Saga, and mana of any type can be spent to cast that spell.
 * Flying, haste
 *
 * - Chapter I is King Narfi's Betrayal's per-graveyard walk, made mandatory: the Saga's controller
 *   (`Chooser.SourceController` — the per-player loop rebinds "you" to the iterated player) picks
 *   one instant or sorcery card from every graveyard that has one, and the picks are exiled
 *   together and linked to this Saga (CR 607.2a, "exiled with this Saga").
 * - Chapters II–IV add the red mana first, then offer one card from the linked pile for a cast
 *   *during resolution* (CR 608.2g; ruling: you can't wait to cast it later), paying its cost with
 *   mana of any type ([Effects.CastFromCollection]'s `withAnyManaType`, CR 118.14) — so the
 *   just-added {R} pays a blue or colorless cost. Picking nothing is the "may". Cards left in the
 *   pile stay castable by a later chapter; a cast card becomes a new object and leaves the pile.
 */
private val instantOrSorcery = GameObjectFilter.InstantOrSorcery

private fun castFromValigarmandaExile() = Effects.Pipeline {
    run(Effects.AddMana(Color.RED, DynamicAmounts.countersOnSelf(CounterType.LORE)))
    val exiled = gather(CardSource.FromLinkedExile())
    val castable = filter(exiled, instantOrSorcery)
    val picked = chooseUpTo(
        1,
        from = castable,
        prompt = "You may cast an instant or sorcery card exiled with Summon: Esper Valigarmanda " +
            "(mana of any type can be spent)",
        selectedLabel = "Cast",
    )
    run(Effects.CastFromCollection(from = picked, withAnyManaType = true))
}

val SummonEsperValigarmanda = card("Summon: Esper Valigarmanda") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment Creature — Saga Drake"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)\n" +
        "I — Exile an instant or sorcery card from each graveyard.\n" +
        "II, III, IV — Add {R} for each lore counter on this Saga. You may cast an instant or sorcery card " +
        "exiled with this Saga, and mana of any type can be spent to cast that spell.\n" +
        "Flying, haste"
    power = 3
    toughness = 3

    keywords(Keyword.FLYING, Keyword.HASTE)

    sagaChapter(1) {
        effect = Effects.Pipeline {
            val (picked) = forEachPlayerCollecting(Player.Each) {
                val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, instantOrSorcery))
                listOf(
                    chooseExactly(
                        1,
                        from = graveyard,
                        chooser = Chooser.SourceController,
                        prompt = "Exile an instant or sorcery card from this graveyard",
                        selectedLabel = "Exile",
                    )
                )
            }
            exile(picked, linkToSource = true)
        }
    }

    for (chapter in 2..4) {
        sagaChapter(chapter) {
            effect = castFromValigarmandaExile()
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "60"
        artist = "Fiona Hsieh"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f0298b4-c7ca-49b8-baaa-b9fad4c2bfd5.jpg?1783906354"
        ruling(
            "2025-06-06",
            "You cast the spell while the ability is resolving and still on the stack. You can't wait to cast " +
                "it later in the turn."
        )
        ruling(
            "2025-06-06",
            "Summon: Esper Valigarmanda's second, third, and fourth chapter abilities aren't mana abilities. " +
                "They use the stack and can be responded to."
        )
    }
}
